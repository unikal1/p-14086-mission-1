# p-14086-mission-1

SimpleDb는 JDBC 환경에서 최소한의 DB 접근 기능을 제공하는 경량 유틸리티입니다.

DriverManager 기반의 단일 커넥션 사용, ThreadLocal 기반 커넥션 재사용, 
간단한 SQL 빌더 및 매핑 기능을 제공하며, ORM 없이 순수 JDBC 방식으로 동작합니다.

## 기능
### 1. DataSource 생성 및 등록 기능 <br>
ConnectionManager를 통해 JDBC URL을 생성하고 SimpleDataSource를 만들며, 이를 DataSourceRegistry에 이름으로 등록하여 조회할 수 있습니다.

### 2. ThreadLocal 기반 커넥션 관리 기능 <br>
SimpleDb는 생성 시 커넥션을 ThreadLocal에 보관하며, 같은 스레드 내에서 동일한 커넥션을 재사용합니다.

### 3. SQL 빌더 및 실행 기능 <br>
Sql 객체를 통해 SQL 문자열을 구성하고, insert, update, delete, select 계열 메서드를 실행할 수 있습니다.

### 4. 단일 행 및 다중 행 조회 기능 <br>
selectRow, selectRows 등을 통해 Map<String, Object> 또는 DTO(Class<T>) 형태의 결과를 조회할 수 있습니다.

### 5. 단일 값 조회 기능 <br>
selectLong, selectString, selectBoolean, selectDatetime 등을 통해 단일 컬럼 값을 조회할 수 있습니다.

### 6. 트랜잭션 기능 <br>
startTransaction, commit, rollback을 통해 단순한 트랜잭션을 처리할 수 있습니다.

### 7. 커넥션 종료 기능 <br>
AutoCloseable을 구현하므로 try-with-resources 문법을 사용할 수 있습니다.

