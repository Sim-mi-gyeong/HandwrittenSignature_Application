package com.me.handwrittensignature;
// Start 페이지(처음 화면) -> 이름 입력(InputName.java)로 이동

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class TestActivity extends AppCompatActivity {

    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);

        iv = findViewById(R.id.image1);
    }

    public void onClick(View view) {
        // Cloud Storage 연결 설정 테스트!!
        //firebaseStorage 인스턴스 생성
        //하나의 Storage와 연동되어 있는 경우, getInstance()의 파라미터는 공백으로 두어도 됨
        //하나의 앱이 두개 이상의 Storage와 연동이 되어있 경우, 원하는 저장소의 스킴을 입력
        //getInstance()의 파라미터는 firebase console에서 확인 가능('gs:// ... ')
        FirebaseStorage storage = FirebaseStorage.getInstance();

        //생성된 FirebaseStorage를 참조하는 storage 생성
        StorageReference storageRef = storage.getReference();

        //Storage 내부의 images 폴더 안의 image.jpg 파일명을 가리키는 참조 생성
        StorageReference pathReference = storageRef.child("images/image.jpg");

        pathReference = storageRef.child("dog.jpg");

        if (pathReference != null) {
            // 참조 객체로부터 이미지 다운로드 url을 얻어오기
            pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    // 다운로드 URL이 파라미터로 전달되어 옴.
                    Glide.with(TestActivity.this).load(uri).into(iv);

                }
            });
        }
    }


//        // Get the data from an ImageView as bytes
//        imageView.setDrawingCacheEnabled(true);
//        imageView.buildDrawingCache();
//        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//        byte[] data = baos.toByteArray();
//
//        UploadTask uploadTask = mountainsRef.putBytes(data);
//        uploadTask.addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception exception) {
//                // Handle unsuccessful uploads
//            }
//        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
//                // ...
//            }
//        });



}