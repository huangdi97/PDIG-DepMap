import conformanceHostTest from './ConformanceHost.test';
import domainHostTest from './DomainHost.test';
import persistenceHostTest from './PersistenceHost.test';
export default function testsuite() {
  conformanceHostTest();
  domainHostTest();
  persistenceHostTest();
}
