import { ScopeList } from './ScopeList';

export default {
    name: 'scopes',
    list: ScopeList,
    recordRepresentation: record => record.scope,
};
