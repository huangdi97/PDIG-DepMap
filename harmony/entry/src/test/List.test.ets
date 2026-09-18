import conformanceHostTest from './ConformanceHost.test';
import domainHostTest from './DomainHost.test';

export default function testsuite() {
  conformanceHostTest();
  domainHostTest();
}
