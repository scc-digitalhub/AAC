import { ApiResourceIcon } from './ApiResourceIcon';
import { ApiResourcesList } from './ApiResourcesList';

export default {
    name: 'resources',
    list: ApiResourcesList,
    icon: ApiResourceIcon,
    recordRepresentation: record => record.name,
};
