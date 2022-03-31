package com.example.photoapi2;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static java.util.UUID.randomUUID;
import static org.springframework.http.MediaType.parseMediaType;

@RestController
public class FileController {



    @PostMapping("/images")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(storeFile(file));
    }

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<?> downloadFile(@PathVariable("filename") String filename)  {

        try {
            S3Object s3object = getFile(filename);
            if (s3object == null){
                return ResponseEntity.ok().body("File could not be retrieved.");
            }
            //ObjectInputStream temp = new ObjectInputStream(fileResource.getInputStream());
            //String contentType = URLConnection.guessContentTypeFromStream(fileResource.getInputStream());
            ObjectMetadata meta = s3object.getObjectMetadata();
            String contentType = meta.getContentType();

            S3ObjectInputStream inputStream = s3object.getObjectContent();
            Resource fileResource = new InputStreamResource(inputStream);
            //String mimeType = tika.detect(fileResource);
            if (contentType == null)
                contentType = "application/octet-stream";
            //    FileSystemResource resource = (FileSystemResource) fileResource;
            return ResponseEntity.ok()
                    .contentType(parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
                    .body(fileResource);
        }
        catch (AmazonServiceException e){
            System.out.println(e);
        }
        return ResponseEntity.ok().body("File could not be retrieved.");
    }

    @GetMapping("/images")
    public String listFiles() throws JSONException {

        final AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        JSONObject json = new JSONObject();

        ObjectListing objects = s3client.listObjects("photo-boi");
        int i = 0;
        try {
            if (objects != null && objects.getObjectSummaries() != null) {

                for (S3ObjectSummary summary : objects.getObjectSummaries()) {
                    json.put(("image " + i), summary.getKey());
                    i++;
                    if (i == 50) {
                        break;
                    }
                }
            }
        }
        catch (AmazonServiceException e){
            json.put("error",e);
            return json.toString();
        }

        return json.toString();

    }

    public String storeFile(MultipartFile
                                    multipartFile) throws IOException {
        String filename = String.valueOf(randomUUID());
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        try {
            ObjectMetadata data = new ObjectMetadata();
            data.setContentType(multipartFile.getContentType());
            data.setContentLength(multipartFile.getSize());
            s3.putObject("photo-boi", filename, multipartFile.getInputStream(), data);
        }
        catch (AmazonServiceException e){
            filename = e.toString();
        }
        return filename;

    }

    public S3Object getFile(String filename) {
        S3Object s3object=null;
        try {
            final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

            s3object = s3.getObject("photo-boi", filename);
        }
        catch(AmazonServiceException e){
            System.out.println(e);
        }



        return s3object;


    }



}
