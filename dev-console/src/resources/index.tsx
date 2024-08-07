import { ApiResourceIcon } from './ApiResourceIcon';
import { ApiResourcesList } from './ApiResourcesList';

export default {
    list: ApiResourcesList,
    icon: ApiResourceIcon,
    recordRepresentation: record => record.name,
};
