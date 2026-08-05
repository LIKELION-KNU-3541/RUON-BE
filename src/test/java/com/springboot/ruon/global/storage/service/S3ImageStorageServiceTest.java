package com.springboot.ruon.global.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.springboot.ruon.global.storage.config.S3Properties;
import com.springboot.ruon.global.storage.exception.ImageDeleteException;
import com.springboot.ruon.global.storage.exception.ImageDownloadException;
import com.springboot.ruon.global.storage.exception.ImageNotFoundException;
import com.springboot.ruon.global.storage.exception.ImageUploadException;
import com.springboot.ruon.global.storage.exception.ImageValidationException;
import com.springboot.ruon.global.storage.key.ImageObjectKeyGenerator;
import com.springboot.ruon.global.storage.validation.ImageFileValidator;
import com.springboot.ruon.global.storage.validation.ImageFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageServiceTest {

    private static final String BUCKET = "test-bucket";

    @Mock
    private S3Client s3Client;

    private S3ImageStorageService service;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties("ap-northeast-2", BUCKET);
        service = new S3ImageStorageService(
                s3Client, properties, new ImageFileValidator(), new ImageObjectKeyGenerator());
    }

    /** 형식별 유효한 매직 바이트로 시작하는 테스트 데이터를 만든다. */
    private static byte[] validBytes(ImageFormat format) {
        return switch (format) {
            case JPEG -> new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
            case PNG -> new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
            case WEBP -> new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        };
    }

    private MockMultipartFile validJpeg() {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg", validBytes(ImageFormat.JPEG));
    }

    @ParameterizedTest
    @CsvSource({
            "photo.jpg, image/jpeg, JPEG, jpg",
            "photo.png, image/png, PNG, png",
            "photo.webp, image/webp, WEBP, webp"
    })
    @DisplayName("정상 업로드 시 objectKey를 반환하고 형식별 버킷·키·Content-Type을 함께 전송한다")
    void uploadReturnsObjectKey(String filename, String contentType, ImageFormat format, String extension) {
        byte[] bytes = validBytes(format);
        MockMultipartFile file = new MockMultipartFile("image", filename, contentType, bytes);

        String objectKey = service.upload(1L, file);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest request = captor.getValue();

        assertThat(objectKey).startsWith("scans/1/").endsWith("." + extension);
        assertThat(request.bucket()).isEqualTo(BUCKET);
        assertThat(request.key()).isEqualTo(objectKey);
        assertThat(request.contentType()).isEqualTo(contentType);
        assertThat(request.contentLength()).isEqualTo((long) bytes.length);
    }

    @Test
    @DisplayName("검증에 실패한 파일은 S3에 업로드하지 않는다")
    void invalidFileIsNotUploaded() {
        MockMultipartFile invalid = new MockMultipartFile("image", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.upload(1L, invalid))
                .isInstanceOf(ImageValidationException.class);
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("업로드 중 SDK 예외는 ImageUploadException으로 변환된다")
    void uploadSdkExceptionIsTranslated() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("connection failed"));

        assertThatThrownBy(() -> service.upload(1L, validJpeg()))
                .isInstanceOf(ImageUploadException.class)
                .hasCauseInstanceOf(SdkClientException.class);
    }

    @Test
    @DisplayName("정상 조회 시 byte[]를 반환한다")
    void downloadReturnsBytes() {
        byte[] data = {10, 20, 30};
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), data));

        byte[] result = service.download("scans/1/some-key.jpg");

        assertThat(result).isEqualTo(data);
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo("scans/1/some-key.jpg");
    }

    @Test
    @DisplayName("존재하지 않는 objectKey 조회는 ImageNotFoundException으로 변환된다")
    void downloadMissingKeyIsTranslated() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("no such key").build());

        assertThatThrownBy(() -> service.download("scans/1/missing.jpg"))
                .isInstanceOf(ImageNotFoundException.class);
    }

    @Test
    @DisplayName("조회 중 그 외 SDK 예외는 ImageDownloadException으로 변환된다")
    void downloadSdkExceptionIsTranslated() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(SdkClientException.create("connection failed"));

        assertThatThrownBy(() -> service.download("scans/1/some-key.jpg"))
                .isInstanceOf(ImageDownloadException.class);
    }

    @Test
    @DisplayName("정상 삭제 시 버킷과 objectKey로 deleteObject를 호출한다")
    void deleteCallsS3() {
        service.delete("scans/1/some-key.jpg");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo("scans/1/some-key.jpg");
    }

    @Test
    @DisplayName("삭제 중 SDK 예외는 ImageDeleteException으로 변환된다")
    void deleteSdkExceptionIsTranslated() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("connection failed"));

        assertThatThrownBy(() -> service.delete("scans/1/some-key.jpg"))
                .isInstanceOf(ImageDeleteException.class);
    }

    @Test
    @DisplayName("null·빈 objectKey로 조회·삭제하면 IllegalArgumentException이 발생한다")
    void blankObjectKeyIsRejected() {
        assertThatThrownBy(() -> service.download(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.download(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.delete(""))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(s3Client);
    }
}
