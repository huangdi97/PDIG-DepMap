import conformanceHostTest from './ConformanceHost.test';
import domainHostTest from './DomainHost.test';
import persistenceHostTest from './PersistenceHost.test';
import repositoryHostTest from './RepositoryHost.test';
import importPipelineHostTest from './ImportPipelineHost.test';
import coreJourneyHostTest from './CoreJourneyHost.test';
export default function testsuite() {
  conformanceHostTest();
  domainHostTest();
  persistenceHostTest();
  repositoryHostTest();
  importPipelineHostTest();
  coreJourneyHostTest();
}
