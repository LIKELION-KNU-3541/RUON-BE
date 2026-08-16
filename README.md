<div align="center">

## ✨ 나를 위한 루틴을 켜다, RUON

### 임신·수유 중에도 안심할 수 있는 AI 피부 관리 루틴

화장품 전성분을 분석하고 사용자의 상태와 화장대 제품을 바탕으로  
개인 맞춤형 아침·저녁 피부 관리 루틴을 제공합니다.

<br>

</div>

## 🔄 Service Flow

<div align="center">
  <img
    src="./docs/images/ruon-service-flow.svg"
    width="1000"
    alt="RUON 서비스 처리 흐름"
  >
</div>

<br>

## 💥 Main Features

<table>
    <tr>
      <td width="50%" valign="middle">

  ### 📷 01. 화장품 정보 인식

  사용자가 화장품 성분표 이미지를 촬영하면 이
  미지가 안전하게 저장되고, OCR을 통해 제품의
  텍스트 정보를 추출합니다.

  - 화장품 이미지 업로드
  - AWS S3 비공개 저장
  - Google Vision OCR
  - 제품명·브랜드·용량·전성분 구조화

  <br>
  </td>
      <td width="50%" valign="middle">

  ### 🔍 02. 임신·수유 주의 성분 분석

  추출한 전성분을 식약처 기반 성분 정보와 RAG
  데이터베이스에서 검색하여 사용자의 상태에 맞
  는 정보를 제공합니다.

  - 임신·수유 단계 반영
  - 주의 성분 및 판단 근거 제공
  - 알레르기·자극 가능성 확인
  - 정보가 부족한 성분 별도 안내

  <br>
  </td>
    </tr>
    <tr>
      <td width="50%" valign="middle">

  ### 🧴 03. 개인 화장대 관리

  분석이 완료된 화장품을 개인 화장대에 등록하
  고 사용 여부와 분석 현황을 관리합니다.

  - 보유 화장품 등록 및 조회
  - 제품 사용 상태 변경
  - 분석 결과별 제품 분류
  - 사용자 상태 변경 시 재분석

  <br>
  </td>
      <td width="50%" valign="middle">

  ### 🌿 04. AI 맞춤 루틴 생성

  사용자의 현재 상태와 화장대에 등록된 제품을
  종합하여 맞춤형 아침·저녁 루틴을 생성합니다.

  - 보유 제품만 활용
  - 제품 사용 순서와 방법 제공
  - 중복·주의 성분 조합 확인
  - 사용 가능 시간에 따른 단계 조절

  <br>
  </td>
    </tr>
  </table>

<br>

## 🛡️ Product Classification

<div align="center">

| 분류 | 의미 |
|:---:|:---|
| 🟢 **사용 유지** | 현재 상태에서 사용할 수 있는 제품 |
| 🟡 **선택 사용** | 피부 상태에 따라 선택적으로 사용할 제품 |
| 🟠 **잠시 보류** | 현재 상태에서는 사용을 미루는 것이 권장되는 제품 |
| ⚪ **추가 확인** | 정보가 부족하여 전문가 확인이 필요한 제품 |

</div>

<br>

## 🌿 Git Branch Strategy

프로젝트에서는 브랜치를 역할에 따라 다음과 같이 분리했습니다.

| Branch | Description |
|:---|:---|
| `main` | 배포 가능한 안정 버전 관리 |
| `develop` | 개발이 완료된 기능 통합 |
| `feat/#이슈번호` | 기능 개발 |
| `fix/#이슈번호` | 버그 수정 |
| `refactor/#이슈번호` | 코드 구조 개선 |

> 각 작업은 기능 단위로 **GitHub Issue**를 생성한 후 최신 `develop`에서 브랜치를 생성하여 진행합니다.  
> 작업 완료 후 `develop`을 대상으로 **Pull Request**를 생성하고, 팀원 리뷰를 거쳐 **Merge commit** 방식으로 병합합니다.

<br>

<div align="center">

## 👥 Backend Team

<img
  src="https://github.githubassets.com/images/icons/emoji/unicode/1f981.png"
  width="70"
  alt="사자">

<table>
  <tr>
    <td align="center" width="200">
      <a href="https://github.com/tangerinem">
        <img
          src="https://github.com/tangerinem.png"
          width="130"
          alt="김민재"
        >
      </a>
      <br><br>
      <b>김민재</b>
      <br>
      <sub>강남대학교 멋쟁이사자처럼 14기</sub>
      <br><br>
      <a href="https://github.com/tangerinem">@tangerinem</a>
    </td>
    <td align="center" width="200">
      <a href="https://github.com/wogus21">
        <img
          src="https://github.com/wogus21.png"
          width="130"
          alt="재현"
        >
      </a>
      <br><br>
      <b>재현</b>
      <br>
      <sub>강남대학교 멋쟁이사자처럼 14기</sub>
      <br><br>
      <a href="https://github.com/wogus21">@wogus21</a>
    </td>
    <td align="center" width="200">
      <a href="https://github.com/Garden0728">
        <img
          src="https://github.com/Garden0728.png"
          width="130"
          alt="최정원"
        >
      </a>
      <br><br>
      <b>최정원</b>
      <br>
      <sub>강남대학교 멋쟁이사자처럼 14기</sub>
      <br><br>
      <a href="https://github.com/Garden0728">@Garden0728</a>
    </td>
  </tr>
</table>

<br>

### 🦁 RUON Backend Team

</div>
