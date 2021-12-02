package com.me.handwrittensignature;
// ForgerySign_Skilled

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ForgerySign_Skilled_Practice extends AppCompatActivity {
    private TextView modeText;
    private SignaturePad signaturePad;
    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.skilled_forgery_sign_practice);

        TextView modeText = (TextView)findViewById(R.id.modeText);
        Button loadButton = (Button)findViewById(R.id.loadButton);
        Button startButton = (Button)findViewById(R.id.button_start);
        Button restartButton = (Button)findViewById(R.id.button_restart);
        Button endButton = (Button)findViewById(R.id.button_end);

        modeText.setVisibility(View.VISIBLE);

        iv = findViewById(R.id.image1);

//        saveButton.setVisibility(false)
//        clearButton.setVisibility(false);

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {

            @Override
            public void onStartSigning() {
                //Event triggered when the pad is touched

//                clearButton.setVisibility(true);
//                saveButton.setVisibility(true);
            }

            @Override
            public void onSigned() {
                //Event triggered when the pad is signed
                restartButton.setEnabled(true);
                endButton.setEnabled(true);
            }

            public void onClear() {
                //Event triggered when the pad is cleared
                restartButton.setEnabled(false);
                endButton.setEnabled(false);
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //불러오기 버튼 숨기기
                loadButton.setEnabled(false);

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
                            Glide.with(ForgerySign_Skilled_Practice.this).load(uri).into(iv);

                        }
                    });
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.setEnabled(true);   // 서명 패드 활성화

                loadButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);   // 시작 버튼 숨기기
                restartButton.setVisibility(View.VISIBLE);   // 초기화 버튼 나타나게
                endButton.setVisibility(View.VISIBLE);   // 종료 버튼 나타나게
                startButton.setEnabled(false);   // 시작 버튼 비활성화
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                restartButton.setEnabled(true);
                endButton.setEnabled(true);
            }
        });

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Skilled 연습 페이지 종료 시  skilled 위조 서명  등록 화면으로
                Intent intent = new Intent(getApplicationContext(), ForgerySign_Skilled.class);
                startActivity(intent);
            }
        });

    }

//    public void onClick(View view) {
//        // Cloud Storage 연결 설정 테스트!!
//        //firebaseStorage 인스턴스 생성
//        //하나의 Storage와 연동되어 있는 경우, getInstance()의 파라미터는 공백으로 두어도 됨
//        //하나의 앱이 두개 이상의 Storage와 연동이 되어있 경우, 원하는 저장소의 스킴을 입력
//        //getInstance()의 파라미터는 firebase console에서 확인 가능('gs:// ... ')
//        FirebaseStorage storage = FirebaseStorage.getInstance();
//
//        //생성된 FirebaseStorage를 참조하는 storage 생성
//        StorageReference storageRef = storage.getReference();
//
//        //Storage 내부의 images 폴더 안의 image.jpg 파일명을 가리키는 참조 생성
//        StorageReference pathReference = storageRef.child("images/image.jpg");
//
//        pathReference = storageRef.child("dog.jpg");
//
//        if (pathReference != null) {
//            // 참조 객체로부터 이미지 다운로드 url을 얻어오기
//            pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//                @Override
//                public void onSuccess(Uri uri) {
//                    // 다운로드 URL이 파라미터로 전달되어 옴.
//                    Glide.with(ForgerySign_Skilled_Practice.this).load(uri).into(iv);
//
//                }
//            });
//        }
//    }

}