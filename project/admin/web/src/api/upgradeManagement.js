import request from '@/utils/request.js'

export const getUpgradeConfig = () =>
  request.get('/settings/upgrade-management/config').then((res) => res.data)

export const updateUpgradeConfig = (payload) =>
  request.put('/settings/upgrade-management/config', payload).then((res) => res.data)

export const listUpgradeScripts = () =>
  request.get('/settings/upgrade-management/scripts').then((res) => res.data)

export const executeUpgradeScript = (scriptRelativePath, args = []) =>
  request.post('/settings/upgrade-management/actions/execute', { scriptRelativePath, arguments: args }).then((res) => res.data)

export const listUpgradeJobs = (params) =>
  request.get('/settings/upgrade-management/jobs', { params }).then((res) => res.data)

export const getUpgradeJob = (id) =>
  request.get(`/settings/upgrade-management/jobs/${id}`).then((res) => res.data)
