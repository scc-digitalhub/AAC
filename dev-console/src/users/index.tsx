import { UserCreate } from './UserCreate';
import { UserIcon } from './UserIcon';
import { UserList } from './UserList';
import { UserShow } from './UserShow';

export default {
    list: UserList,
    create: UserCreate,
    show: UserShow,
    icon: UserIcon,
    recordRepresentation: record => record.username,
};
