import conformanceHostTest from './ConformanceHost.test';
import domainHostTest from './DomainHost.test';
import persistenceHostTest from './PersistenceHost.test';
import repositoryHostTest from './RepositoryHost.test';
export default function testsuite() {
  conformanceHostTest();
  domainHostTest();
  persistenceHostTest();
  repositoryHostTest();
}
