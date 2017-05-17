package default;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Component
@Log4j
public class AmazonS3ClientFacade {

    @Value("${cf.fileToUploadPath}")
    public String fileToUploadPath;


    public void createFileAndCleanup(Map<String, Object> credentials) throws InterruptedException, AmazonClientException {
        AmazonS3 s3client = createClient(credentials);
        String bucketName = UUID.randomUUID().toString();
        String key = UUID.randomUUID().toString();
        try {
            createFileAndWait(s3client, bucketName, key);
        } finally {
            deleteFileAndBucket(s3client, bucketName, key);
        }
    }

    private void deleteFileAndBucket(AmazonS3 s3client, String bucketName, String key) {
        s3client.deleteObject(bucketName, key);
        s3client.deleteBucket(bucketName);
    }

    private AmazonS3 createClient(Map<String, Object> credentials) {
        AmazonS3 s3client = new AmazonS3Client(new BasicAWSCredentials(credentials.get("accessKey").toString(), credentials.get("sharedSecret").toString()));
        s3client.setEndpoint("https://" + credentials.get("accessHost"));
        return s3client;
    }


    public void createFileAndWait(AmazonS3 s3client, String bucketName, String key) throws InterruptedException, AmazonClientException {
        Bucket bucket = s3client.createBucket(bucketName);
        validateTheUploadFile();
        s3client.putObject(new PutObjectRequest(bucketName, key, new File(fileToUploadPath)));
        Thread.sleep(5800);
    }

    private void validateTheUploadFile() {
        if (!new File(fileToUploadPath).canRead()) {
            throw new NoSuchElementException("Unable to read the file");
        }
    }

}
