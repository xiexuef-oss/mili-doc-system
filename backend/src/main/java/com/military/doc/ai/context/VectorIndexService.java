package com.military.doc.ai.context;

import com.military.doc.ai.config.EmbeddingProperties;
import com.military.doc.ai.entity.EmbeddingIndexTask;
import com.military.doc.ai.llm.EmbeddingClient;
import com.military.doc.ai.mapper.EmbeddingIndexTaskMapper;
import com.military.doc.modules.knowledge.entity.KnowledgeBase;
import com.military.doc.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.military.doc.modules.standard.entity.StandardClause;
import com.military.doc.modules.standard.mapper.StandardClauseMapper;
import com.military.doc.modules.standard.mapper.StandardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class VectorIndexService {

    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties embeddingProperties;
    private final StandardClauseMapper clauseMapper;
    private final StandardMapper standardMapper;
    private final KnowledgeBaseMapper kbMapper;
    private final EmbeddingIndexTaskMapper taskMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final int PAGE_SIZE = 50;

    public VectorIndexService(EmbeddingClient embeddingClient,
                              EmbeddingProperties embeddingProperties,
                              StandardClauseMapper clauseMapper,
                              StandardMapper standardMapper,
                              KnowledgeBaseMapper kbMapper,
                              EmbeddingIndexTaskMapper taskMapper,
                              JdbcTemplate jdbcTemplate) {
        this.embeddingClient = embeddingClient;
        this.embeddingProperties = embeddingProperties;
        this.clauseMapper = clauseMapper;
        this.standardMapper = standardMapper;
        this.kbMapper = kbMapper;
        this.taskMapper = taskMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---- Bulk indexing ----

    @Async
    public void indexAllClausesAsync(EmbeddingIndexTask task) {
        try {
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            int total = 0;
            int completed = 0;
            int failed = 0;
            int offset = 0;
            while (true) {
                List<StandardClause> page = clauseMapper.selectList(
                    new LambdaQueryWrapper<StandardClause>()
                        .orderByAsc(StandardClause::getId)
                        .last("LIMIT " + PAGE_SIZE + " OFFSET " + offset)
                );
                if (page.isEmpty()) break;
                total += page.size();

                List<String> texts = new ArrayList<>();
                List<Long> ids = new ArrayList<>();
                for (StandardClause c : page) {
                    String text = buildClauseText(c);
                    texts.add(text);
                    ids.add(c.getId());
                }

                try {
                    List<float[]> embeddings = embeddingClient.embedBatch(texts);
                    for (int i = 0; i < ids.size(); i++) {
                        String hash = sha256(texts.get(i));
                        upsertClauseEmbedding(ids.get(i), embeddings.get(i), hash);
                        completed++;
                    }
                } catch (Exception e) {
                    log.error("Batch embed failed at offset {}: {}", offset, e.getMessage());
                    failed += texts.size();
                }

                offset += PAGE_SIZE;
                task.setTotalCount(total);
                task.setCompletedCount(completed);
                task.setFailedCount(failed);
                taskMapper.updateById(task);

                if (page.size() < PAGE_SIZE) break;
            }

            task.setStatus("COMPLETED");
            task.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Index all clauses failed", e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
        }
        taskMapper.updateById(task);
    }

    @Async
    public void indexAllKnowledgeAsync(EmbeddingIndexTask task) {
        try {
            task.setStatus("RUNNING");
            task.setStartedAt(LocalDateTime.now());
            taskMapper.updateById(task);

            int total = 0;
            int completed = 0;
            int failed = 0;
            int offset = 0;
            while (true) {
                List<KnowledgeBase> page = kbMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeBase>()
                        .orderByAsc(KnowledgeBase::getId)
                        .last("LIMIT " + PAGE_SIZE + " OFFSET " + offset)
                );
                if (page.isEmpty()) break;
                total += page.size();

                List<String> texts = new ArrayList<>();
                List<Long> ids = new ArrayList<>();
                for (KnowledgeBase kb : page) {
                    String text = buildKbText(kb);
                    texts.add(text);
                    ids.add(kb.getId());
                }

                try {
                    List<float[]> embeddings = embeddingClient.embedBatch(texts);
                    for (int i = 0; i < ids.size(); i++) {
                        String hash = sha256(texts.get(i));
                        upsertKbEmbedding(ids.get(i), embeddings.get(i), hash);
                        completed++;
                    }
                } catch (Exception e) {
                    log.error("Batch embed KB failed at offset {}: {}", offset, e.getMessage());
                    failed += texts.size();
                }

                offset += PAGE_SIZE;
                task.setTotalCount(total);
                task.setCompletedCount(completed);
                task.setFailedCount(failed);
                taskMapper.updateById(task);

                if (page.size() < PAGE_SIZE) break;
            }

            task.setStatus("COMPLETED");
            task.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Index all knowledge failed", e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
        }
        taskMapper.updateById(task);
    }

    // ---- Single-item reindex ----

    public void reindexClause(Long clauseId) {
        StandardClause clause = clauseMapper.selectById(clauseId);
        if (clause == null) return;
        String text = buildClauseText(clause);
        try {
            float[] emb = embeddingClient.embed(text);
            upsertClauseEmbedding(clauseId, emb, sha256(text));
            jdbcTemplate.update(
                "UPDATE standard_clause SET embedding_indexed = TRUE WHERE id = ?", clauseId);
            log.info("Reindexed clause {}", clauseId);
        } catch (Exception e) {
            log.error("Failed to reindex clause {}: {}", clauseId, e.getMessage());
        }
    }

    public void reindexKnowledge(Long kbId) {
        KnowledgeBase kb = kbMapper.selectById(kbId);
        if (kb == null) return;
        String text = buildKbText(kb);
        try {
            float[] emb = embeddingClient.embed(text);
            upsertKbEmbedding(kbId, emb, sha256(text));
            jdbcTemplate.update(
                "UPDATE knowledge_base SET embedding_indexed = TRUE WHERE id = ?", kbId);
            log.info("Reindexed knowledge {}", kbId);
        } catch (Exception e) {
            log.error("Failed to reindex knowledge {}: {}", kbId, e.getMessage());
        }
    }

    // ---- Semantic search ----

    public List<SemanticMatch> searchSimilarClauses(String queryText, int topK) {
        if (queryText == null || queryText.isBlank()) return List.of();
        try {
            float[] queryVec = embeddingClient.embed(queryText);
            String vectorStr = toPgVector(queryVec);

            String sql = """
                SELECT sc.id, sc.clause_number, sc.clause_title, sc.clause_content, sc.keywords,
                       s.standard_code, s.standard_name,
                       (1 - (sce.embedding <=> ?::vector)) AS similarity
                FROM standard_clause_embedding sce
                JOIN standard_clause sc ON sc.id = sce.clause_id AND sc.deleted = 0
                JOIN standard s ON s.id = sc.standard_id AND s.deleted = 0 AND s.status = 'ACTIVE'
                WHERE sce.embedding IS NOT NULL
                ORDER BY sce.embedding <=> ?::vector
                LIMIT ?
                """;

            return jdbcTemplate.query(sql,
                (rs, rowNum) -> new SemanticMatch(
                    rs.getLong("id"),
                    rs.getString("clause_number"),
                    rs.getString("clause_title"),
                    rs.getString("clause_content"),
                    rs.getString("keywords"),
                    rs.getString("standard_code"),
                    rs.getString("standard_name"),
                    null,
                    rs.getDouble("similarity")
                ),
                vectorStr, vectorStr, topK);
        } catch (Exception e) {
            log.error("Semantic clause search failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<SemanticMatch> searchSimilarKnowledge(String queryText, int topK) {
        if (queryText == null || queryText.isBlank()) return List.of();
        try {
            float[] queryVec = embeddingClient.embed(queryText);
            String vectorStr = toPgVector(queryVec);

            String sql = """
                SELECT kb.id, kb.title, kb.content, kb.category,
                       (1 - (kbe.embedding <=> ?::vector)) AS similarity
                FROM knowledge_base_embedding kbe
                JOIN knowledge_base kb ON kb.id = kbe.kb_id AND kb.deleted = 0
                WHERE kbe.embedding IS NOT NULL AND kb.status = 'ACTIVE'
                ORDER BY kbe.embedding <=> ?::vector
                LIMIT ?
                """;

            return jdbcTemplate.query(sql,
                (rs, rowNum) -> new SemanticMatch(
                    rs.getLong("id"),
                    null,
                    rs.getString("title"),
                    rs.getString("content"),
                    null,
                    null,
                    null,
                    rs.getString("category"),
                    rs.getDouble("similarity")
                ),
                vectorStr, vectorStr, topK);
        } catch (Exception e) {
            log.error("Semantic knowledge search failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ---- Stats and task queries ----

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("embeddingModel", embeddingProperties.getModel());
        stats.put("dimension", embeddingProperties.getDimension());
        stats.put("semanticRagEnabled", embeddingProperties.isSemanticRagEnabled());

        Integer indexedClauses = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM standard_clause_embedding WHERE embedding IS NOT NULL", Integer.class);
        Integer totalClauses = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM standard_clause WHERE deleted = 0", Integer.class);
        stats.put("indexedClauses", indexedClauses != null ? indexedClauses : 0);
        stats.put("totalClauses", totalClauses != null ? totalClauses : 0);

        Integer indexedKb = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM knowledge_base_embedding WHERE embedding IS NOT NULL", Integer.class);
        Integer totalKb = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM knowledge_base WHERE deleted = 0", Integer.class);
        stats.put("indexedKnowledge", indexedKb != null ? indexedKb : 0);
        stats.put("totalKnowledge", totalKb != null ? totalKb : 0);

        return stats;
    }

    public List<EmbeddingIndexTask> getRecentTasks() {
        return taskMapper.selectList(
            new LambdaQueryWrapper<EmbeddingIndexTask>()
                .orderByDesc(EmbeddingIndexTask::getCreatedAt)
                .last("LIMIT 20")
        );
    }

    public EmbeddingIndexTask createTask(String taskType, String targetTable) {
        EmbeddingIndexTask task = new EmbeddingIndexTask();
        task.setTaskType(taskType);
        task.setTargetTable(targetTable);
        task.setStatus("PENDING");
        taskMapper.insert(task);
        return task;
    }

    // ---- Helpers ----

    private String buildClauseText(StandardClause c) {
        StringBuilder sb = new StringBuilder();
        if (c.getClauseNumber() != null) sb.append(c.getClauseNumber()).append(" ");
        if (c.getClauseTitle() != null) sb.append(c.getClauseTitle()).append(" ");
        if (c.getClauseContent() != null) {
            String content = c.getClauseContent();
            if (content.length() > 2000) content = content.substring(0, 2000);
            sb.append(content);
        }
        return sb.toString().trim();
    }

    private String buildKbText(KnowledgeBase kb) {
        StringBuilder sb = new StringBuilder();
        if (kb.getTitle() != null) sb.append(kb.getTitle()).append(" ");
        if (kb.getCategory() != null) sb.append(kb.getCategory()).append(" ");
        if (kb.getContent() != null) {
            String content = kb.getContent();
            if (content.length() > 2000) content = content.substring(0, 2000);
            sb.append(content);
        }
        return sb.toString().trim();
    }

    private void upsertClauseEmbedding(Long clauseId, float[] embedding, String hash) {
        String vectorStr = toPgVector(embedding);
        String model = embeddingProperties.getModel();
        jdbcTemplate.update(
            "INSERT INTO standard_clause_embedding (clause_id, embedding, model_name, text_hash) " +
            "VALUES (?, ?::vector, ?, ?) " +
            "ON CONFLICT (clause_id) DO UPDATE SET embedding = ?::vector, indexed_at = NOW(), model_name = ?, text_hash = ?",
            clauseId, vectorStr, model, hash,
            vectorStr, model, hash);
        jdbcTemplate.update(
            "UPDATE standard_clause SET embedding_indexed = TRUE WHERE id = ?", clauseId);
    }

    private void upsertKbEmbedding(Long kbId, float[] embedding, String hash) {
        String vectorStr = toPgVector(embedding);
        String model = embeddingProperties.getModel();
        jdbcTemplate.update(
            "INSERT INTO knowledge_base_embedding (kb_id, embedding, model_name, text_hash) " +
            "VALUES (?, ?::vector, ?, ?) " +
            "ON CONFLICT (kb_id) DO UPDATE SET embedding = ?::vector, indexed_at = NOW(), model_name = ?, text_hash = ?",
            kbId, vectorStr, model, hash,
            vectorStr, model, hash);
        jdbcTemplate.update(
            "UPDATE knowledge_base SET embedding_indexed = TRUE WHERE id = ?", kbId);
    }

    static String toPgVector(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
