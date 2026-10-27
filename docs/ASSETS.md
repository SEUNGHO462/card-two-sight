# 이미지·글꼴·효과음

## 런타임 자산

기존 대화에서 제작한 그림을 작업 폴더 output/imagegen에서 복사했습니다. 원본은 변경하지 않았습니다.

- characters: 기존 characters-v1의 투명 배경 6인.
- cards/specials: 기존 final-assets-v1/cards의 역전·정찰·위장·예측.
- backgrounds/casino.png: 기존 메인 시안을 바탕으로 글자·버튼을 제거한 배경.
- revolver/gun.png: 같은 미술 스타일의 투명 배경 소품.
- revolver/wheel.png: 구멍 없는 탄창 판. 실제 9개 탄실은 코드로 표시.
- cards/blank.png: 기존 역전 카드에서 문구·문양을 제거한 숫자 카드 공용 바탕.
- design: 전체 화면 참고 시안. 실제 버튼은 이미지에 포함된 버튼을 재사용하지 않음.

추가 이미지는 내장 image_gen.imagegen으로 생성·편집했습니다. 사용 프롬프트는 ASSET_PROMPTS.txt에 남겼습니다. 게임의 숫자·이름·점수·버튼은 Java로 표시합니다.

## 글꼴

NanumGothic-Regular.ttf: Google Fonts의 Nanum Gothic. 글꼴 라이선스는 assets/fonts/OFL.txt에 포함했습니다.
출처: https://github.com/google/fonts/tree/main/ofl/nanumgothic

## 효과음

tools/make_audio.py로 생성한 짧은 합성 WAV 5개: 클릭·공탄·실탄·회전·승리. 외부 게임에서 추출한 소리는 없습니다. 긴 배경 음악과 전문 성우 음성은 포함하지 않았습니다.

## 사용 전 확인

AI 이미지의 세부 모양과 원근은 개별 자산마다 조금 다릅니다. 공개 출시 단계에서는 팀이 최종 미술 검수와 사용권 검토를 진행하세요. 이 프로젝트는 기존 구룡투의 이미지·로고·상용 음원을 포함하지 않습니다.

## 2026-10-27 메뉴·카드·글꼴 갱신

- assets/ui/menu: 금색 고전 로고, 버건디 장식 버튼, 총탄 흔적, 투명 핏방울 비산 장식. 내장 image_gen.imagegen으로 생성했습니다.
- assets/cards/specials/*-droplets-v2.png 및 cards/blank-droplets-v2.png: 기존 문양과 내용을 유지하고 핏방울 표현을 수정했습니다. GameAssets가 기존 경로를 새 버전으로 연결합니다.
- 특수카드에는 작은 blood-splatter-v1.png 장식을 두 군데 겹쳐 표시합니다. 카드 이름과 설명을 가리지 않도록 가장자리에 배치했습니다.
- 게임의 실시간 글자는 NanumMyeongjo Regular / Bold를 사용합니다. SIL OFL 고지는 assets/fonts/OFL-NanumMyeongjo.txt에 포함했습니다.
- 메뉴 클릭에는 기존 자체 제작 trigger-shot-v2.wav와 클릭 위치의 총탄 흔적을 사용하며, 약 0.32초 후 선택한 화면으로 이동합니다.
- 생성 프롬프트와 파일 대응표: docs/MENU_CARD_ART_PROMPTS.json.
- 결과 화면 누적 기록: 큰 금색 명조 글자, 버건디 바탕, 두께 5의 금색 테두리.

실제 렌더링 미리보기로 메뉴·설정·캐릭터·카드·탄창·누적 기록을 확인했습니다. 오디오 호출은 확인했으나 스피커 출력은 별도로 청취하지 않았습니다.
