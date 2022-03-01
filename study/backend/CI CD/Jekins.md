## Jekins

### 정의

- 소프트웨어 빌드, 테스트, 전송 또는 배포와 관련된 모든 종류의 작업을 자동화하는 데 사용할 수 있는 자체 포함 오픈 소스 자동화 서버
- 네이티브 시스템 패키지, Docker를 통해 설치될 수 있으며 JRE(Java Runtime Environment)가 설치된 모든 컴퓨터에서 독립적으로 실행 가능
- 일반적으로 내장된 Java 서블릿 컨테이너/애플리케이션 서버(Jetty)를 사용하여 자체 프로세스에서 독립 실행형 응용프로그램
- 다른 Java 서블릿 컨테이너에서 서블릿으로 실행 가능

---

> - 빌드
>
>   소스 코드 파일을 컴퓨터에서 실행할 수 있는 독립적인 형태로 변환하는 과정과 그 결과
>
>   컴파일은 빌드의 부분집합이라고 생각하시면 됩니다.
>
>   또한, 빌드 과정을 도와주는 도구를 **Build Tool**이라고 합니다.
>
>   **즉, 컴파일 된 코드를 실제 실행할 수 있는 상태로 만드는 일을 Build 라는 개념으로 생각하시면 됩니다.**
>
>   ​
>
> - 빌드 툴 
>
>   - 빌드 과정을 도와주는 도구
>   - 전처리(preprocessing), 컴파일(Compile), 패키징(packaging), 테스팅(testing), 배포(distribution)
>   - Ant, Maven, Gradle 등이 있음
>
>
> - Build = Complie + 그 외 작업
>
>   Run = Build + 실행
>
>   ​      = (Complie + 그외작업) + 실행



- nightly-build

  CI툴이 등장하기 전에는 일정시간마다 빌드를 실행하는 방식이 일반적이였다. 

  특히 개발자들이 당일 작성한 소스들의 커밋이 모드 끝난 심야 시간대에 이러한 빌드가 타이머에 의해 집중적으로 진행 되었음

- 정기적인 빌드에서 한발 나아가 서브버전, Git 과 같은 버전관리시스템과 연동하여 소스의 커밋을 감지하면 자동적으로 자동화 테스트가 포함된 빌드가 작동되도록 설정 가능

  ​

### 젠킨스가 주는 이점

개발중인 프로젝트에서 커밋은 매우 빈번히 일어나기 때문에 커밋 횟수만큼 빌드를 실행하는 것이 아니라 작업이 큐잉되어 자신이 실행될 차례를 기다리게 된다

코드의 변경과 함께 이뤄지는 이 같은 자동화된 빌드와 테스트 작업들은 다음과 같은 이점 

- 프로젝트 표준 컴파일 환경에서의 컴파일 오류 검출
- 자동화 테스트 수행
- 정적 코드 분석에 의한 코딩 규약 준수여부 체크
- 프로파일링 툴을 이용한 소스 변경에 따른 성능 변화 감시
- 결합 테스트 환경에 대한 배포작업
- 젠킨스 덕분에 웹 인터페이스로 손쉽게 처리 가능

이 외에도 젠킨스는 500여가지가 넘는 플러그인을 온라인으로 간단히 인스톨 할 수 있는 기능을 제공하고 있으며 파이썬과 같은 스크립트를 이용해 손쉽게 자신에게 필요한 기능을 추가 가능



### 자동화 테스트

자동화 테스트는 젠킨스를 사용해야 하는 가장 큰 이유 중 하나이며, 사실상 자동화 테스트가 포함되지 않은 빌드는 CI자체가 불가능하다고 봐도 무방하다. 젠킨스는 Subversion이나 Git과 같은 버전관리시스템과 연동하여 코드 변경을 감지하고 자동화 테스트를 수행하기 때문에 만약 개인이 미처 실시하지 못한 테스트가 있다 하여도 든든한 안전망이 되어준다. 제대로 테스트를 거치지 않은 코드를 커밋하게 되면 화난 젠킨스를 만나게 된다.



### 빌드 파이프라인 구성

2개 이상의 모듈로 구성되는 레이어드 아키텍처가 적용 된 프로젝트에는 그에 따는 빌드 파이프라인 구성이 필요하다. 예를 들면, `도메인 -> 서비스 -> UI`와 같이 각 레이어의 참조 관계에 따라 순차적으로 빌드를 진행하지 않으면 안된다. 젠킨스에서는 이러한 빌드 파이프라인의 구성을 간단하게 지원



`TL:DR`

도커 컨테이너를 사용하여 애플리케이션을 패킹, 배포 및 실행하는 프로세스입니다.

## 도커라이징이란?

Docker를 사용하여 애플리케이션을 실행하는 데 필요한 모든 항목을 애플리케이션에 포장하고 하나의 패키지(컨테이너)로 제공 할 수 있습니다.

한마로 Dockerizing은 Docker 컨테이너를 사용하여 애플리케이션을 패킹, 배포 및 실행하는 프로세스입니다.

## 도커라이징을 사용해야하는 이유

1. Docker는 사용하기 쉽다. 애플리케이션 배포 방식을 단순화하여 소프트웨어의 소스 코드로 배포하지 않고 디스크 이미지로 배포합니다. 다른 모든 사람들이 사용하기 쉽습니다. docker 허브, aws, gcp 같은 클라우드에서도 이미지를 업로드하고 다운로드할 수있는 인프라가 잘 갖춰져있습니다.
2. 빠릅니다.
   Docker 컨테이너는 커널에서 실행되는 샌드 박스 환경입니다. 몇 초 만에 컨테이너를 만들고 실행할 수 있습니다.
3. 재현 가능한 환경을 만들 수 있음
   Docker는 응용 프로그램과 그 기능을 다시 만드는 데 필요한 일관된 작업 환경을 만듭니다. 모든 것을 컨테이너에 래핑하면 빌드하는 애플리케이션이 마찰 없이 다른 장치에서 실행됩니다.

------

1. 도커를 통해서 로컬에서 개발을 완료하고
2. 도커라이징을 통해 이미지를 만들어서 프로덕션에 배포한다.


![KakaoTalk_20220120_212232878](C:\Users\dong\Documents\카카오톡 받은 파일\KakaoTalk_20220120_212232878.png)



Docker와 Jenkins

- https://velog.io/@hind_sight/Docker-Jenkins-%EB%8F%84%EC%BB%A4%EC%99%80-%EC%A0%A0%ED%82%A8%EC%8A%A4%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%9C-Spring-Boot-CICD
- 위의 링크 참조하여 개인 Mac OS로 사용함 