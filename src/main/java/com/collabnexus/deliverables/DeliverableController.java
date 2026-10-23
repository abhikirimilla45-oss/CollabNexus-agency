package com.collabnexus.deliverables;

import com.collabnexus.common.ApiResponse;
import com.collabnexus.common.PageView;
import com.collabnexus.security.SecurityUser;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/deliverables")
public class DeliverableController {

    private final DeliverableService deliverableService;

    public DeliverableController(DeliverableService deliverableService) {
        this.deliverableService = deliverableService;
    }

    @GetMapping
    public ApiResponse<PageView<DeliverableView>> list(@AuthenticationPrincipal SecurityUser user,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) Boolean overdue,
                                                       @RequestParam(required = false) Integer upcomingDays,
                                                       @RequestParam(required = false) Long collaborationId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(deliverableService.list(user.getId(), user.getRole(), status, overdue,
                upcomingDays, collaborationId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeliverableView> detail(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id) {
        return ApiResponse.ok(deliverableService.detail(user.getId(), user.getRole(), id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<DeliverableView> start(@AuthenticationPrincipal SecurityUser user,
                                              @PathVariable Long id) {
        return ApiResponse.ok(deliverableService.start(user.getId(), id),
                "Deliverable moved into progress.");
    }

    @PostMapping(value = "/{id}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DeliverableView> submit(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id,
                                               @RequestParam(required = false) String submissionUrl,
                                               @RequestParam(required = false) String submissionNotes,
                                               @RequestPart(required = false) MultipartFile file) {
        return ApiResponse.ok(deliverableService.submit(user.getId(), id, submissionUrl,
                submissionNotes, file), "Deliverable submitted for brand review.");
    }

    @GetMapping("/{id}/submissions/{submissionId}/file")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal SecurityUser user,
                                             @PathVariable Long id,
                                             @PathVariable Long submissionId) {
        DeliverableService.Download download = deliverableService.download(user.getId(),
                user.getRole(), id, submissionId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename() == null ? "supporting-file" : download.filename(),
                        StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }
}
