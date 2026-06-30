package com.military.doc.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.military.doc.common.exception.BusinessException;
import com.military.doc.modules.document.entity.DocFile;
import com.military.doc.modules.document.entity.DocLedger;
import com.military.doc.modules.document.mapper.DocFileMapper;
import com.military.doc.modules.document.mapper.DocLedgerMapper;
import com.military.doc.modules.project.entity.Project;
import com.military.doc.modules.project.entity.ProjectMember;
import com.military.doc.modules.project.mapper.ProjectMapper;
import com.military.doc.modules.project.mapper.ProjectMemberMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Checks that the authenticated caller is allowed to touch a project's resources:
 * ROLE_ADMIN, the project's creator, or an ACTIVE project_member row. Controllers across
 * document/project/template/config-management previously only checked "is logged in" —
 * this is the shared gate to retrofit ownership checks without duplicating the
 * project/member lookup in every controller.
 */
@Component
public class ProjectAccessGuard {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final DocLedgerMapper docLedgerMapper;
    private final DocFileMapper docFileMapper;

    public ProjectAccessGuard(ProjectMapper projectMapper, ProjectMemberMapper memberMapper,
                               DocLedgerMapper docLedgerMapper, DocFileMapper docFileMapper) {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.docLedgerMapper = docLedgerMapper;
        this.docFileMapper = docFileMapper;
    }

    public void requireMember(Long projectId, Authentication authentication) {
        if (projectId == null) {
            throw BusinessException.validation("projectId 不能为空");
        }
        if (isAdmin(authentication)) return;
        Long userId = (Long) authentication.getPrincipal();

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.notFound("项目不存在: id=" + projectId);
        }
        if (userId.equals(project.getCreatedBy())) return; // creator always has access

        Long count = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getUserId, userId)
            .eq(ProjectMember::getStatus, "ACTIVE"));
        if (count == null || count == 0) {
            throw new BusinessException("FORBIDDEN", "您不是该项目成员，无权操作");
        }
    }

    /** Resolves the ledger's owning project, checks membership, and returns the ledger (avoids a second fetch). */
    public DocLedger requireMemberForLedger(Long docLedgerId, Authentication authentication) {
        DocLedger ledger = docLedgerMapper.selectById(docLedgerId);
        if (ledger == null) {
            throw BusinessException.notFound("文档台账不存在: id=" + docLedgerId);
        }
        requireMember(ledger.getProjectId(), authentication);
        return ledger;
    }

    /** Resolves the file's owning project, checks membership, and returns the file (avoids a second fetch). */
    public DocFile requireMemberForFile(Long docFileId, Authentication authentication) {
        DocFile file = docFileMapper.selectById(docFileId);
        if (file == null) {
            throw BusinessException.notFound("文档文件不存在: id=" + docFileId);
        }
        requireMember(file.getProjectId(), authentication);
        return file;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
