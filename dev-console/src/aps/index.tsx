import { ApEdit } from './ApEdit';
import { ApIcon } from './ApIcon';
import { ApList } from './ApList';

export default {
    name: 'aps',
    list: ApList,
    edit: ApEdit,
    icon: ApIcon,
    recordRepresentation: record => record.name,
};
