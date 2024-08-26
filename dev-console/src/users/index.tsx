import { UserCreate } from './UserCreate';
import { UserEdit } from './UserEdit';
import { UserIcon } from './UserIcon';
import { UserList } from './UserList';

export default {
    list: UserList,
    create: UserCreate,
    edit: UserEdit,
    icon: UserIcon,
    recordRepresentation: record => record.username,
};
