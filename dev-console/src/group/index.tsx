import { GroupEdit } from './GroupEdit';
import { GroupIcon } from './GroupIcon';
import { GroupList } from './GroupList';

export default {
    list: GroupList,
    edit: GroupEdit,
    icon: GroupIcon,
    recordRepresentation: record => record.group,
};
