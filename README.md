# HandwrittenSignature_Application
</br>

## ✍️ 요약

- 필기체 사인은 작성자가 디스플레이 화면에 입력 가능한 필기도구를 사용하여 작성한 디지털 서명으로, 수기로 작성된 개인의 고유 서명이다.
- 수기로 작성된 필기체 사인 데이터를 직접 개발한 데이터 수집용 어플리케이션을 통해 수집하고, 회전, 샤프닝, 블러링 등의 전처리를 활용한 데이터 증강을 통해 데이터 셋을 구축하도록 한다.
- 궁극적으로는, 필기체 사인 이미지로부터 객체를 식별하여 스마트 이력 추적 및 공유주방 안전 관리에 암호화 기능을 위한 인식 방법론을 설계하기 위함이며, 이를 위해 필기체 사인 이미지로부터 객체를 식별하고, 위변조 유무를 판별해야 하며, 또한 새로운 사용자에 대한 필기체 사인 인식이 가능해야 한다.
- 해당 프로젝트는 필기체 사인 인식 방법론 구현을 위한 데이터 수집을 목적으로 개발된 필기체 서명 데이터 수집용 어플리케이션 개발에 대한 것이다.

</br></br>

## 시연 영상
[구글 드라이브 연결](https://drive.google.com/file/d/1-82cTcEN5uWbL43na9Al50JxedXsoVd_/view?usp=sharing)

</br></br>

## 🛠 사용 기술 및 라이브러리

- Java, Android </br>
 
    - 개발 환경 세팅
     
        - Local 환경 세팅
         
            - Windows 10 (64bits)
            - Android Studio 4.2.1
            - Compile SDK Version: API 30/Android: 11.0(R)
            - minSdkVersion: API 22(Android 5.1 (Lollipop))
            - Gradle Version: 6.7.1
            - Virtual Device: Pixel 2 API 29
            - Test Device: Galaxy Tab S7
            - JDK 15.0.2
           
        - Library
            - Signature-pad: 1.2.1
- Firebase


</br></br>

## 📇 데이터 수집 아키텍처

- 데이터 수집 프로세스 아키텍처

![전체 수집 아키텍처.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/ac248e64-0352-42bc-9044-02b57ab9e4e4/전체_수집_아키텍처.png)

- 각 등록 서명 종류에 따른 프로세스 아키텍처

    - 실제 서명
        
        ![실제 서명 수집 아키텍처.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/9f872196-8fe7-4ac9-8405-e2438dbc01bc/실제_서명_수집_아키텍처.png)
        
    - Unskilled 위조 서명
        
        ![위조 서명 등록 아키텍처.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/a3455b18-12d8-4176-93b5-8bfd60d7fbea/위조_서명_등록_아키텍처.png)
        
    - Skilled 위조 서명
        
        ![Skilled 위조 서명 등록 아키텍처.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/7c6a1dc5-6e33-4dfe-aaae-b1237fca4a69/Skilled_위조_서명_등록_아키텍처.png)
        
        
</br></br>


## 🖼️ 화면 설계

- 사용자 확인/등록 및 서명 등록 화면으로 연결을 위한 화면
    
     ![사용자 등록 화면.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/b10ab7b1-3b77-4f5c-84db-2b27a85228eb/사용자_등록_화면.png)
<!--     https://user-images.githubusercontent.com/80315847/170109594-930ba503-1302-4576-8eac-0efc8e30412d.png -->
<!--     <img width="80%" src="https://user-images.githubusercontent.com/80315847/170109594-930ba503-1302-4576-8eac-0efc8e30412d.png"/> -->
    
- 실제 서명 등록 화면
    
    ![실제 서명 등록 화면.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/9c33609b-b009-4dbb-bb17-ee1ce220db79/실제_서명_등록_화면.png)
    
- Unskilled 위조 서명 등록
    
    ![unskilled 위조 서명 등록 화면.png](https://s3-us-west-2.amazonaws.com/secure.notion-static.com/275ecd44-06e2-43b6-bfeb-82be5071d5e1/unskilled_위조_서명_등록_화면.png)
    
- Skilled 위조 서명 등록


</br></br>

- 사용자 등록 및 확인
    - 자신의 이름 입력 및 이전에 등록된 사용자인지 확인
    - 등록되지 않은 사용자의 경우, 해당 사용자가 기록한 서명 데이터를 담을 디렉토리 생성

</br>    

- 실제 서명 등록
    - 사용자 자신의 실제 서명 등록
    - 서명 등록 시 Timer 기능에 따른 제한 시간 설정
    - 입력 중인 서명의 스트림 데이터 확보를 위한 초당 50 프레임 이상의 캡처
    - 개별 서명 표시를 위한 디렉토리 생성 및 초기화 버튼 사용에 따른 이미지 구분 표시

</br>    

- Unskilled 위조 서명 등록
    - 실제 서명이 등록된 사용자 리스트 중 위조할 대상의 랜덤 선택 및 대상의 랜덤 서명 불러오기 기능
    - 사용자가 위조 대상의 서명을 등록
    - 서명 등록 시 Timer 기능에 따른 제한 시간 설정
    - 입력 중인 서명의 스트림 데이터 확보를 위한 초당 50 프레임 이상의 캡처
    - 개별 서명 표시를 위한 디렉토리 생성 및 초기화 버튼 사용에 따른 이미지 구분 표시
    - 위조 서명을 등록 시 위조 대상의 위조 서명 디렉토리 내 해당 서명 기록 저장

</br> 

- Skiiled 위조 서명 등록
    - 실제 서명이 등록된 사용자 리스트 중 위조할 대상의 랜덤 선택 및 대상의 랜덤 서명 불러오기 기능
    - 불러온 서명에 대해 위조를 위한 서명 연습
    - 서명 연습 이후 바로 위조 서명 등록 페이지로 연결
    - 이전 페이지에서 연습을 한 위조 대상의 동일한 서명 불러오기
    - 사용자가 위조 대상의 서명을 등록
    - 서명 등록 시 Timer 기능에 따른 제한 시간 설정
    - 입력 중인 서명의 스트림 데이터 확보를 위한 초당 50 프레임 이상의 캡처
    - 개별 서명 표시를 위한 디렉토리 생성 및 초기화 버튼 사용에 따른 이미지 구분 표시
    - 위조 서명을 등록 시 위조 대상의 위조 서명 디렉토리 내 해당 서명 기록 저장

</br>

- 서명 기록 시 타이머 설정
    - 데이터 수집 일관성을 확보하기 위한 서명 기록 제한 시간 설정
