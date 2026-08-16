<div align="center">
  <h2>✨ 나를 위한 루틴을 켜다, RUON </h2>
  <p>산모를 위한, 가장 나에게 맞는 피부 관리 루틴을 생성해주는 AI 웰니스 서비스</p>
</div>
<br>

## 💥 Main Feature
<h3> 1. 화장품 전성분 인식 및 제품 정보 등록 </h3>
-  사용자가 화장품 성분표 이미지를 촬영하면 OCR로 텍스트를 추출하고, AI를 통해 브랜드명·제품명·용량·전성분 목록을 구조화합니다.

<h3> 2. 임신·수유 중 주의 성분 분석 </h3>
- OCR로 추출한 전성분을 식약처 기반 성분 데이터와 RAG 데이터베이스에서 대조합니다. <br>
- 각 성분의 임신 안전성, 알레르기 여부, 자극 가능성, 사용 제한 등의 정보를 확인 후 분류합니다. <br>
<u>분류 기준: 사용 유지 / 잠시 보류 / 선택 사용 / 추가 확인</u>

<h3> 3. 개인 화장대 및 분석 현황 관리 </h3>
- 분석이 완료된 화장품을 개인 화장대에 등록하고, 사용 중·사용 중단 상태로 관리할 수 있도록 제공합니다. <br>
- 등록된 화장품을 분석 결과에 따라 집계하여 분류 기준에 해당하는 제품 개수를 한 화면에 제공합니다.

<h3> 4. 개인 맞춤형 데일리 피부 관리 루틴 </h3>
- 다음 정보를 종합하여 개인 맞춤형 아침·저녁 루틴을 생성합니다.

- 임신 단계 및 임신 주차
- 수유 여부
- 오늘의 피부 느낌
- 사용 가능한 루틴 시간
- 개인 화장대에 등록된 제품
- 이전 루틴 및 사용 반응

반드시 **사용자의 화장대에 등록된 제품**만 사용하도록 제한합니다. <br> 
또한 사용 가능 시간에 따라 루틴 단계를 조정하여 짧은 퀵 루틴부터 여유 있는 루틴을 설정할 수 있습니다.

<br> 
<br>

## 🌿 Git Branch Strategy

**프로젝트에서는 브랜치를 역할에 따라 다음과 같이 분리했습니다.**

- `main`: 배포 가능한 안정 버전 관리
- `develop`: 개발이 완료된 기능 통합
- `feat/#이슈번호`: 기능 개발
- `fix/#이슈번호`: 버그 수정
- `refactor/#이슈번호`: 코드 구조 개선

> 각 작업은 기능 단위로 **GitHub Issue**를 생성한 후, 최신 `develop`에서 Issue 번호에 맞는 브랜치를 생성하여 진행.  
> 작업 완료 후 `develop`을 대상으로 **Pull Request**를 생성하고, 팀원 리뷰를 거쳐 **Merge commit** 방식으로 병합.
<br>

<div align="center">
  <h2>👥 Backend Team</h2>
  <p align="center">
  <img src="https://github.githubassets.com/images/icons/emoji/unicode/1f981.png" width="70" alt="사자">
  <br>
</p>
  <br>
  <table align="center" border="1" cellpadding="18" cellspacing="8">
    <tr>
      <td align="center" width="180">
        <a href="https://github.com/tangerinem"><img src="https://github.com/tangerinem.png" width="130" alt="김민재"></a>
        <br><br>
        <b>김민재</b>
        <br>
        <sub>🏫 강남대학교 멋쟁이사자처럼 14기</sub>
        <br><br>
        <a href="https://github.com/tangerinem">@tangerinem</a>
      </td>
      <td align="center" width="180">
        <a href="https://github.com/wogus21"><img src="https://github.com/wogus21.png" width="130" alt="재현"></a>
        <br><br>
        <b>재현</b>
        <br>
        <sub>🏫 강남대학교 멋쟁이사자처럼 14기</sub>
        <br><br>
        <a href="https://github.com/wogus21">@wogus21</a>
      </td>
      <td align="center" width="180">
        <a href="https://github.com/Garden0728"><img src="https://github.com/Garden0728.png" width="130" alt="최정원"></a>
        <br><br>
        <b>최정원</b>
        <br>
        <sub>🏫 강남대학교 멋쟁이사자처럼 14기</sub>
        <br><br>
        <a href="https://github.com/Garden0728">@Garden0728</a>
      </td>
    </tr>
  </table>
</div>
<br>


