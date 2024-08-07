import { RoleList } from './RoleList';
import { RoleEdit } from './RoleEdit';
import { RoleIcon } from './RoleIcon';

export default {
    list: RoleList,
    edit: RoleEdit,
    icon: RoleIcon,
    recordRepresentation: record => record.role,
};
