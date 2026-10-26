# 룰렛 연출 변경 · 2026-10-26

사용 중인 바탕화면 `card-two-sight` 프로젝트에 직접 적용했습니다.

## 바뀐 흐름

- 양쪽 탄실이 확정되면 결과 장면을 시작합니다. 큰 '찰칵/탕!' 문구를 제거했습니다.
- 약 0.85초 준비 후 짧은 금속성 공탄음 또는 실탄 효과음을 재생합니다.
- 공탄은 잠깐 정적 후 두 단계로 총을 내립니다.
- 실탄은 짧은 화면 섬광·흔들림과 함께 놀람 → 뒤로 젖힘 → 기댄 자세를 표시합니다.
- 1.7초 시점에 피격된 사람의 하트가 줄어듭니다. 실제 승패와 목숨은 이미 서버가 판정한 값을 사용합니다.
- 3.15초 이후 다음 라운드 버튼이 활성화됩니다. 상대방의 결과 확인 응답이 와도 연출을 재시작하지 않습니다.

## 이미지 방식과 한계

6인 각각 3열×2행 자세 시트 한 장을 사용합니다. 총 36개 셀이며 몸통·팔·총을 개별 뼈대로 움직이는 방식 대신 자세별 그림을 전환하고 짧게 교차 표시합니다. 60프레임 손그림 애니메이션은 아닙니다.

일부 머리에 총을 대는 자세는 이미지 생성 도구가 거절했습니다. 추가 제작은 총구를 아래로 둔 준비·반응 자세로 변경했습니다. 도윤·레나도 게임에서는 머리에 닿지 않는 어깨 자세를 준비 프레임으로 사용합니다. 피나 상처는 추가하지 않았습니다.

### 저장된 이미지

- assets/characters/roulette/male_handsome-poses-v1.png
- assets/characters/roulette/female_leather-poses-v1.png
- assets/characters/roulette/female_vest-poses-v1.png
- assets/characters/roulette/male_veteran-poses-v1.png
- assets/characters/roulette/female_silver-poses-v1.png
- assets/characters/roulette/male_punk-poses-v1.png

내장 `image_gen.imagegen`을 사용했습니다. 각 원본 참조·최종 저장 위치·실제 프롬프트 전문은 `ROULETTE_ASSET_PROMPTS.json`에 있습니다. 기존 캐릭터 원본은 그대로 유지했습니다.

## 코드와 효과음

- core/ui/RouletteDuel.java: 이미지 프레임·좌우 구분·섬광·효과음
- core/game/ShotTimeline.java: 연출 순서와 목숨 표시 시점
- core/screens/RouletteScreen.java: 결과 장면·다음 단계 잠금·흔들림
- assets/audio/trigger-empty-v2.wav / trigger-shot-v2.wav: 직접 합성한 새 효과음
- tools/make_roulette_audio_v2.py: 효과음 제작 원본

## 확인

규칙·통신·기록 및 새 타이밍 테스트 총 14개 통과. `--shot-preview` 검증 모드에서 6명의 준비·섬광·반동·목숨 감소·결과 장면을 렌더링하고, 같은 결과가 다시 수신되어도 연출이 초기화되지 않는지 확인했습니다. `--smoke`는 연습 게임 전체 흐름을 확인합니다. 화면 검증은 무음으로 진행하며 실제 스피커 음색은 기기에서 확인해야 합니다.

## 추가 변경: 나머지 4인 자세 v2

서린·태식·준은 몸에서 떨어뜨린 어깨 옆 준비 자세 → 총 내리기 → 놀람 → 뒤로 젖힘 → 기댄 자세로 새로 제작했습니다. 마담 류는 무장 자세의 생성도 거절되어 빈손의 준비·반응 자세로 제작했습니다. 머리에 총을 대는 자세를 새로 생성하거나 합성하지 않았습니다.

- assets/characters/roulette/female_vest-poses-v2.png
- assets/characters/roulette/male_veteran-poses-v2.png
- assets/characters/roulette/female_silver-poses-v2.png
- assets/characters/roulette/male_punk-poses-v2.png

기존 v1 파일은 보관하고, v2가 있는 인물만 새 파일을 사용합니다. 탄창 선택 화면의 변경 제안은 보류했습니다. 내장 image_gen.imagegen으로 제작했으며 실제 프롬프트와 원본·저장 위치는 ROULETTE_ASSET_PROMPTS_V2.json에 있습니다. 빌드·14개 기존 테스트 및 네 인물의 실제 자세 전환 렌더링을 확인했습니다.
## 마담 류 추가 수정 v3

사용자 요청에 따라 여섯 자세 모두 오른손에 리볼버를 추가하고 총구를 몸에서 떨어진 바깥쪽 위로 향하게 했습니다. assets/characters/roulette/female_silver-poses-v3.png를 사용하며, v2 빈손 이미지는 보관합니다. 내장 image_gen.imagegen 편집을 사용했으며 프롬프트·원본·최종 경로는 SILVER_UPWARD_POSES_V3.json에 있습니다. 빌드·14개 테스트·실제 게임 렌더링을 확인했습니다.