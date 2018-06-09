package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class S3UploadSevice extends IntentService {

    private static final int MEGABYTES_PER_PART = 5;
    private String clientRegion;
    private String bucketName;
    private String keyName;
    private String filepath;
    private AmazonS3Client s3Client;
    private Uri uploadUri;
    private SharedPreferences sharedPreferences;
    int mNotificationId = 101;
    private static final String ACTION_UPLOAD_NOW = "upload_now";
    private static final String UPLOAD_URI = "upload_uri";
    private String userId;

    public S3UploadSevice() {
        super("S3UploadSevice");
    }

    public S3UploadSevice(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
            Log.d(getClass().getSimpleName(), "insideOnHandleIntent");
            sharedPreferences = getSharedPreferences("uploads", MODE_PRIVATE);
            userId = sharedPreferences.getString("FIREBASE_USER_ID", null);
            clientRegion = Region.getRegion(Regions.AP_SOUTH_1).getName();
            bucketName = getString(R.string.s3bucketname);
            uploadUri = intent.getParcelableExtra(UPLOAD_URI);
            filepath = uploadUri.toString().substring(uploadUri.toString().lastIndexOf('/'));

            if (BuildConfig.DEBUG)
                keyName = "debug/" + userId + filepath;
            else
                keyName = "logs/" + userId + filepath;

            Log.d(getClass().getSimpleName(), keyName);
            Log.d(getClass().getSimpleName(), filepath);
            Log.d(getClass().getSimpleName(), bucketName);

            s3Client = new AmazonS3Client(Util.getsCredProvider(this));

            //each ETag identifies the different parts into which the file has been split
            List<PartETag> partETags = new ArrayList<>();
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
            InitiateMultipartUploadResult initResult = null;
            File file = new File(uploadUri.getPath());
            CompleteMultipartUploadRequest completeMultipartUploadRequest = null;
            List<String> uploadIdList = new ArrayList<>();

            long contentLength = file.length();
            long partSize = MEGABYTES_PER_PART * 1024 * 1024;

            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; ++i) {
                try {
                    initResult = s3Client.initiateMultipartUpload(initRequest);
                    Log.d(getClass().getSimpleName(), "part " + i);
                    partSize = Math.min(partSize, (contentLength - filePosition));
                    UploadPartRequest uploadPartRequest = new UploadPartRequest()
                            .withBucketName(bucketName)
                            .withKey(keyName)
                            .withUploadId(initResult.getUploadId())
                            .withPartNumber(i)
                            .withFileOffset(filePosition)
                            .withFile(file)
                            .withPartSize(partSize);

                    UploadPartResult uploadPartResult = s3Client.uploadPart(uploadPartRequest);
                    partETags.add(uploadPartResult.getPartETag());
                    filePosition += partSize;
                    uploadIdList.add(initResult.getUploadId());
                    Log.d(getClass().getSimpleName(), partETags.toString());
                    } catch (AmazonServiceException e) {
                    //returned when the client sends the call to upload, but S3 cannot process it
                    Log.e(getClass().getSimpleName(), e.getMessage());
                }
            }
            for (int i = 0; i < uploadIdList.size(); ++i) {
                completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                        bucketName, keyName, uploadIdList.get(i), partETags
                );
            }
            if (completeMultipartUploadRequest != null) {
                s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        }
    }
}