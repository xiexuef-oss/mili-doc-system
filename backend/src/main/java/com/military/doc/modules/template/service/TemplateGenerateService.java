package com.military.doc.modules.template.service;

import com.military.doc.common.storage.FileStorageService;
import com.military.doc.modules.document.entity.DocFile;
import com.military.doc.modules.document.entity.DocVersion;
import com.military.doc.modules.document.mapper.DocFileMapper;
import com.military.doc.modules.document.mapper.DocVersionMapper;
import com.military.doc.modules.template.entity.DocTemplate;
import com.military.doc.modules.template.mapper.DocTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class TemplateGenerateService {

    @Autowired
    private DocTemplateMapper templateMapper;

    @Autowired
    private DocFileMapper docFileMapper;

    @Autowired
    private DocVersionMapper docVersionMapper;

    @Autowired
    private FileStorageService fileStorageService;

    public DocFile generate(Long templateId, Long projectId, Map<String, String> variables) {
        DocTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getFileObjectId() == null) {
            throw new RuntimeException("模版或模版文件不存在");
        }

        // Read template content
        String content;
        try (InputStream is = fileStorageService.download(template.getFileObjectId())) {
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取模版文件失败", e);
        }

        // Substitute variables {{key}} → value
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }

        // Create DocFile
        DocFile docFile = new DocFile();
        docFile.setProjectId(projectId);
        docFile.setDocName(template.getTemplateName());
        docFile.setDocType(template.getTemplateType());
        docFile.setSecurityLevel("INTERNAL");
        docFile.setStatus("DRAFT");
        docFileMapper.insert(docFile);

        // Upload generated content as file and create DocVersion
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String objectId = fileStorageService.upload(new ByteArrayMultipartFile(
            template.getFileName() != null ? template.getFileName() : "generated.docx",
            bytes
        ));

        DocVersion version = new DocVersion();
        version.setDocFileId(docFile.getId());
        version.setVersionNo("1.0");
        version.setSourceType("TEMPLATE");
        version.setFileObjectId(objectId);
        version.setVersionStatus("DRAFT");
        version.setChangeSummary("从模版「" + template.getTemplateName() + "」生成");
        docVersionMapper.insert(version);

        return docFile;
    }

    // In-memory MultipartFile adapter for byte array
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final byte[] content;

        ByteArrayMultipartFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return "application/octet-stream"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
