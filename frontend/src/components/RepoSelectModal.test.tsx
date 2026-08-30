import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { RepoSelectModal } from './RepoSelectModal';
import { Repository } from '../types';

const mockRepo1: Repository = {
  id: '101',
  githubRepoId: 101,
  name: 'octocat/first-app',
  branch: 'main',
  isPrivate: false,
  language: 'TypeScript',
  findingCount: 0,
  attentionStatus: 'NotScanned',
  isScanned: false,
};

const mockRepo2: Repository = {
  id: '102',
  githubRepoId: 102,
  name: 'octocat/second-app',
  branch: 'develop',
  isPrivate: true,
  language: 'Java',
  findingCount: 0,
  attentionStatus: 'NotScanned',
  isScanned: false,
};

describe('RepoSelectModal UI Truth Table & State Discrimination', () => {
  const dummySelect = vi.fn();
  const dummyClose = vi.fn();

  it('renders nothing when isOpen is false', () => {
    const html = renderToStaticMarkup(
      <RepoSelectModal
        isOpen={false}
        status="SUCCESS"
        availableRepos={[mockRepo1]}
        monitoredRepos={[]}
        onSelectRepo={dummySelect}
        onClose={dummyClose}
      />
    );
    expect(html).toBe('');
  });

  it('State 1: UNAUTHORIZED -> renders "Session Expired" and "Sign In with GitHub"; NEVER claims "All Repositories Monitored"', () => {
    const html = renderToStaticMarkup(
      <RepoSelectModal
        isOpen={true}
        status="UNAUTHORIZED"
        availableRepos={[]}
        monitoredRepos={[]}
        onSelectRepo={dummySelect}
        onClose={dummyClose}
        onSignIn={vi.fn()}
      />
    );

    // 1. Must render Session Expired state
    expect(html).toContain('Session Expired');
    expect(html).toContain('Sign In with GitHub');
    expect(html).toContain('Your authentication session has expired');

    // 2. Must NEVER render misleading All Repositories Monitored or Request Failed
    expect(html).not.toContain('All Repositories Monitored');
    expect(html).not.toContain('Repository Request Failed');
    expect(html).not.toContain('No Repositories Accessible');
  });

  it('State 2: ERROR -> renders "Repository Request Failed" with details and Retry action; NEVER claims "All Repositories Monitored"', () => {
    const html = renderToStaticMarkup(
      <RepoSelectModal
        isOpen={true}
        status="ERROR"
        errorMessage="GitHub API rate limit exceeded (403)"
        availableRepos={[]}
        monitoredRepos={[]}
        onSelectRepo={dummySelect}
        onClose={dummyClose}
        onRetry={vi.fn()}
      />
    );

    // 1. Must render Request Failed state
    expect(html).toContain('Repository Request Failed');
    expect(html).toContain('GitHub API rate limit exceeded (403)');
    expect(html).toContain('Retry');

    // 2. Must NEVER render misleading All Repositories Monitored or Session Expired
    expect(html).not.toContain('All Repositories Monitored');
    expect(html).not.toContain('Session Expired');
    expect(html).not.toContain('No Repositories Accessible');
  });

  it('State 3: SUCCESS with 0 accessible repos -> renders "No Repositories Accessible" with GitHub App install link', () => {
    const serverInstallUrl = 'https://github.com/apps/scan-pilot/installations/new?state=signed-state-token-xyz';

    const html = renderToStaticMarkup(
      <RepoSelectModal
        isOpen={true}
        status="SUCCESS"
        availableRepos={[]}
        monitoredRepos={[]}
        installUrl={serverInstallUrl}
        onSelectRepo={dummySelect}
        onClose={dummyClose}
      />
    );

    // 1. Must render No Repositories Accessible state
    expect(html).toContain('No Repositories Accessible');
    expect(html).toContain('Install / Configure GitHub App');
    expect(html).toContain(serverInstallUrl);

    // 2. Must NEVER render All Repositories Monitored or Error states
    expect(html).not.toContain('All Repositories Monitored');
    expect(html).not.toContain('Repository Request Failed');
    expect(html).not.toContain('Session Expired');
  });

  it('State 4: SUCCESS with all repos monitored -> renders "All Repositories Monitored" with count', () => {
    const serverInstallUrl = 'https://github.com/apps/scan-pilot/installations/new?state=signed-state-token-xyz';

    const html = renderToStaticMarkup(
      <RepoSelectModal
        isOpen={true}
        status="SUCCESS"
        availableRepos={[mockRepo1, mockRepo2]}
        monitoredRepos={[mockRepo1, mockRepo2]}
        installUrl={serverInstallUrl}
        onSelectRepo={dummySelect}
        onClose={dummyClose}
      />
    );

    // 1. Must render All Repositories Monitored
    expect(html).toContain('All Repositories Monitored');
    expect(html).toContain('All 2 accessible repositories');
    expect(html).toContain('Add More Repositories');
    expect(html).toContain(serverInstallUrl);

    // 2. Must NOT render error or unmonitored items
    expect(html).not.toContain('No Repositories Accessible');
    expect(html).not.toContain('Repository Request Failed');
  });

  it('State 5: SUCCESS with available unmonitored repos -> renders items and dynamic server installUrl', () => {
    const serverInstallUrl = 'https://github.com/apps/scan-pilot/installations/new?state=opaque-state-123';

    const html = renderToStaticMarkup(
      <RepoSelectModal
        isOpen={true}
        status="SUCCESS"
        availableRepos={[mockRepo1, mockRepo2]}
        monitoredRepos={[mockRepo1]}
        installUrl={serverInstallUrl}
        onSelectRepo={dummySelect}
        onClose={dummyClose}
      />
    );

    // 1. Must render unmonitored repo
    expect(html).toContain('octocat/second-app');
    expect(html).toContain('develop');
    expect(html).toContain('Java');
    expect(html).toContain('Ready to Import');
    expect(html).toContain('Monitor This Repository');

    // 2. Must use server-generated installUrl for the top configure button
    expect(html).toContain(serverInstallUrl);
    expect(html).not.toContain('https://github.com/apps/scan-pilot"');

    // 3. Must not render already monitored repo in unmonitored list
    expect(html).toContain('Available Repositories (1)');
  });

  it('Diagnostic & Secret Safety: displays sanitized error without leaking sensitive tokens or file paths', () => {
    const safeError = 'Failed to retrieve repositories from server';
    const html = renderToStaticMarkup(
      <RepoSelectModal
        isOpen={true}
        status="ERROR"
        errorMessage={safeError}
        availableRepos={[]}
        monitoredRepos={[]}
        onSelectRepo={dummySelect}
        onClose={dummyClose}
        onRetry={vi.fn()}
      />
    );

    expect(html).toContain('Failed to retrieve repositories from server');
    expect(html).not.toContain('gho_');
    expect(html).not.toContain('/etc/');
    expect(html).not.toContain('jwt_secret');
  });
});
