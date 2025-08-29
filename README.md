# 옷장을 부탁해 (Otvoo)

> 개인화 의상 및 아이템 추천 SaaS를 위한 백엔드 API 서버 프로젝트

<br>

## 프로젝트 개요 (Overview)

이 프로젝트는 날씨와 취향을 고려하여 사용자가 보유한 의상을 조합해 추천해주고, OOTD 피드 및 팔로우/DM 등의 소셜 기능을 제공하는 **개인화 패션 추천 서비스**입니다.  
RESTful API를 통해 클라이언트와 통신하며, 안정적이고 확장 가능한 구조를 목표로 합니다.

- **프로젝트 목표**: 개인 맞춤형 의상 추천 및 소셜 피드 기능을 제공하는 SaaS 구축
- **주요 기능**:
    - 사용자 관리 (회원가입, 로그인, 소셜 로그인, 권한 관리, 계정 잠금)
    - 프로필 관리 (이미지, 이름, 성별, 생년월일, 위치, 온도 민감도)
    - 의상 관리 (등록, 속성 정의, 구매 링크 기반 정보 추출)
    - 날씨 데이터 수집 및 알림 (Spring Batch 기반, 기상청 API 활용)
    - 추천 알고리즘 기반 의상 추천 (LLM API 활용 가능)
    - OOTD 피드 (게시, 좋아요, 댓글, 알림)
    - 팔로우 및 DM (실시간 채팅, 웹소켓 기반)
    - 알림 시스템 (SSE 기반)
- **개발 기간**: 2025.07.28 ~ 2025.08.30

<br>

## 팀원 (Team Members)

| 이름 | GitHub |
| :---: | :---: |
| 공한나 | [@HANNAKONG](https://github.com/HANNAKONG) |
| 도효림 | [@coderimspace](https://github.com/coderimspace) |
| 양성준 | [@GogiDosirak](https://github.com/GogiDosirak) |
| 안재관 | [@kkwan99](https://github.com/kkwan99) |
| 이경빈 | [@Leekb0804](https://github.com/Leekb0804) |
| 이유나 | [@nayu-yuna](https://github.com/nayu-yuna) |
<br>

## 기술 스택 (Tech Stack)

<table>
  <tr>
    <th>분류</th>
    <th>스택</th>
  </tr>
  <tr>
    <td>언어 (Language)</td>
    <td><img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white"></td>
  </tr>
  <tr>
    <td>프레임워크 (Framework)</td>
    <td><img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"></td>
  </tr>
  <tr>
    <td>보안 (Security)</td>
    <td><img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> <img src="https://img.shields.io/badge/jjwt-BC4521?style=for-the-badge&logo=jjwt&logoColor=white"></td>
  </tr>
  <tr>
    <td>데이터 엑세스 (Data Access)</td>
    <td><img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white">  <img src="https://img.shields.io/badge/QueryDSL-007ACC?style=for-the-badge&logo=java&logoColor=white">  <img src="https://img.shields.io/badge/JDBC-007396?style=for-the-badge&logo=java&logoColor=white"></td>
  </tr>
    <tr>
    <td>데이터베이스 (Database)</td>
    <td><img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">  <img src="https://img.shields.io/badge/Redis-D22128?style=for-the-badge&logo=redis&logoColor=white">  <img src="https://img.shields.io/badge/Elasticsearch-249?style=for-the-badge&logo=elasticsearch&logoColor=white"></td>
  </tr>
  <tr>
    <td>배치 (Batch)</td>
    <td><img src="https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
  </tr>
  <tr>
    <td>빌드 도구 (Build Tool)</td>
    <td><img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"></td>
  </tr>
  <tr>
    <td>클라우드/인프라 (Infra)</td>
    <td><img src="https://img.shields.io/badge/Amazon_ECS-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white"> <img src="https://img.shields.io/badge/Amazon_EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white"> <img src="https://img.shields.io/badge/Amazon_S3-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white"> <img src="https://img.shields.io/badge/Amazon_RDS-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white"> <img src="https://img.shields.io/badge/Amazon_SQS-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white"> <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"></td>
  </tr>
  <tr>
    <td>협업 (Cooperation)</td>
    <td><img src="https://img.shields.io/badge/Jira-326CE5?style=for-the-badge&logo=jira&logoColor=white"> <img src="https://img.shields.io/badge/Github-222?style=for-the-badge&logo=githubr&logoColor=white"></td>
  </tr>
</table>


<br>

## 실행 환경 설정 (Getting Started)

### 사전 요구사항 (Prerequisites)
- Docker (docker desktop)

### 설치 및 실행 (Installation)

1.  **Git 저장소 클론**
    ```bash
    git clone https://github.com/team1-otvoo/sb02-otvoo-team1.git
    ```
    
    clone후에 build.gradle 파일에서
    ```bash
    tasks.named('bootJar') {
    exclude("static/**")
    }
    # 위 부분 주석처리할 것
    ```


2.  **AWS 리소스 준비 (IAM 사용자 및 S3 버킷)**
    - 애플리케이션이 S3에 접근하기 위해 필요한 S3 버킷과 IAM 사용자의 Access Key를 준비합니다. 
      - S3 버킷 생성: 파일을 저장할 S3 버킷을 생성하고 버킷 이름과 리전(Region)을 기억해두세요.
      - IAM 사용자 생성:
        - 보안을 위해 최소한의 권한( s3:GetObject, s3:PutObject, s3:DeleteObject)을 가진 IAM 사용자를 생성합니다.
        - 생성이 완료되면 Access Key ID 와 Secret Access Key 를 발급받아 안전한 곳에 보관하세요.


3.  **.env.example 파일 생성**
    - 먼저, 프로젝트 최상위 경로(root)에 아래 내용으로 .env.example 파일을 만들어주세요.
         { } 로 감싸진 부분은 직접 입력해야 합니다.

           ```bash
          # .env.example
        
          # Spring Security
          SPRING_SECURITY_COOKIE_SECURE=false
        
          # Google OAuth 2.0 (OAuth 기능 사용 시 필요 - https://developers.google.com/identity/sign-in/web/sign-in?hl=ko 해당 링크 참고)
          GOOGLE_CLIENT_ID={your-google-client-id}
          GOOGLE_CLIENT_SECRET={your-google-client-secret}
          GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
        
          # Kakao OAuth 2.0 (OAuth 기능 사용 시 필요 - https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api 해당 링크 참고)
          KAKAO_CLIENT_ID={your-kakao-client-id}
          KAKAO_REDIRECT_URI=http://localhost:8080/login/oauth2/code/kakao
          KAKAO_REST_API_KEY={your-kakao-rest-api-key}
        
          # PostgreSQL Database
          POSTGRES_DB=otvoo_db
          POSTGRES_USERNAME=otvoo_user
          POSTGRES_PASSWORD=otvoo_password
        
          # Elasticsearch
          ELASTICSEARCH_URIS_LOCAL=http://elasticsearch:9200
        
          # OpenAI API (의상 추천 서비스, 이미지 url로 옷 등록시 사용 - openAI 에서 직접 key 발급하여 사용)
          OPENAI_API_KEY={your-openai-api-key}
        
          # Email (SMTP) (비밀 번호 찾기시 사용 - 구글 email 과 app 비밀번호 사용)
          MAIL_HOST=smtp.gmail.com
          MAIL_PORT=587
          MAIL_USERNAME={your-email@gmail.com}
          MAIL_PASSWORD={your-gmail-app-password}
        
          # Weather API (날씨 정보 조회시 사용 - 기상청 api key 사용)
          WEATHER_API_KEY={your-weather-api-key}
        
          # Admin User
          ADMIN_NAME=admin
          ADMIN_EMAIL=admin@example.com
          ADMIN_PASSWORD=admin_password
        
          # AWS S3
          S3_BUCKET={your-s3-bucket-name}
          S3_REGION={your-s3-bucket-region}
          AWS_ACCESS_KEY_ID={your-aws-access-key-id}
          AWS_SECRET_ACCESS_KEY={your-aws-secret-access-key}
          S3_PRESIGNED_TTL=600
       
          AWS_REGION={your-aws-region}
           ```


    

4.  **실행**
    - 아래 명령어로 애플리케이션을 실행합니다.
        ```bash
        docker-compose up -d
        ```

5.  **실행 확인**
    - 애플리케이션이 정상적으로 실행되면 `http://localhost:8080` 으로 접속할 수 있습니다.

<br>

## 프로젝트 구조 (Project Structure)

```
src
└── main
    └── java
        └── com.team1.otvoo
			├── auth # 인증 및 인가 (로그인, JWT 등)
			├── clothes # 의상 관리 (등록, 속성, 링크 기반 정보 추출)
			├── comment # 댓글 관리
			├── config # 전역 설정 (Security, Swagger 등)
			├── directmessage # DM(Direct Message) 실시간 채팅 (웹소켓)
			├── exception # 커스텀 예외 및 예외 처리
			├── feed # OOTD 피드 (게시, 좋아요, 댓글)
			├── follow # 팔로우 기능
			├── interceptor # 인터셉터 (요청/응답 가로채기)
			├── notification # 알림 시스템 (SSE)
			├── recommendation # 의상 추천 알고리즘
			├── security # 보안 관련 설정 (필터, 인증 처리)
			├── sqs # AWS SQS 연동 (비동기 메시징)
			├── sse # Server Sent Event 처리
			├── storage # 파일/이미지 저장소 관리
			├── user # 사용자 관리 (회원가입, 프로필 등)
			├── weather # 날씨 데이터 관리 (기상청 API, 배치)
			└── OtvooApplication # Spring Boot 메인 실행 클래스
```

<br>

## 브랜치 전략 및 커밋 규칙

### 브랜치 전략 (Branch Strategy)
- `main`: 메인 릴리즈 브랜치
- `develop`: 다음 릴리즈를 위한 개발 브랜치
- `feature/[Jira키]-[기능명]`: 신규 기능 개발 브랜치
    - 예: `feature/KAN-111-login-api`
- `fix/[Jira키]-[이슈명]`: 버그 수정 브랜치
- `refactor/[Jira키]-[이슈명]`: 코드 리팩토링 브랜치
- `chore/[Jira키]-[이슈명]`: 빌드, 환경설정 관련 브랜치
- `test/[Jira키]-[이슈명]`: 테스트 코드 추가/수정 브랜치
- 모든 브랜치는 **Jira 티켓 키(KAN-XXX)를 포함**해야 합니다.

### 커밋 메시지 컨벤션 (Commit Convention)
- `[태그]: KAN-000 작업 요약` 형식으로 작성
- 예시: `feat: KAN-111 로그인 API 구현`
- **태그 종류**:
    - `feat`: 새로운 기능 추가
    - `fix`: 버그 수정
    - `docs`: 문서 수정
    - `style`: 코드 포맷팅, 세미콜론 누락 등 (기능 변경 없음)
    - `refactor`: 코드 리팩토링
    - `test`: 테스트 코드 추가/수정
    - `chore`: 빌드, 환경설정 등 유지보수
- **커밋 단위**는 Task 기준 (기능 1개 ≒ 2~5커밋)
- 모든 커밋은 Jira Task와 연결되어야 합니다.

<br>

## 관련 링크 (Links)

- **Notion**: [otvoo 프로젝트 노션 페이지](https://mewing-pheasant-8da.notion.site/242a6cd811ce80719a45c1fe4f089f1a?source=copy_link)