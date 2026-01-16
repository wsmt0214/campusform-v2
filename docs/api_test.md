# 복사해서 자유롭게 테스트하기 위함함

# SQL
select * from USERS;
select * from APPLICANTS;
select * from APPLICANT_EXTRA_ANSWERS;
select * from PROJECTS;
select * from PROJECT_ADMINS;

# 관리자 찾기 호출 예시시
/api/users/exists?email=iht@naver.com

# 프로젝트 생성
/api/projects?ownerId=1

{
    "title": "프로젝트 테스트 명",
    "sheetUrl": "https://docs.google.com/spreadsheets/d/1PFbbmFXXg2WCaKBJCGJL3XoEiZ71zAA7GKJIGNMh-0M/edit?resourcekey=&gid=1307587132#gid=1307587132",
    "startAt": "2024-08-01",
    "endAt": "2024-11-01",
    "adminIds": [1, 2],
    "requiredMappings": {
        "nameIdx": 1,
        "schoolIdx": -1,
        "majorIdx": -1,
        "genderIdx": -1,
        "phoneIdx": 3,
        "emailIdx": 2,
        "positionIdx": -1
    }
}
(참고로 -1의 의미는 매핑을 하지 않겠다는 뜻)