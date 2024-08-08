import { AppEdit } from './AppEdit';
import { AppIcon } from './AppIcon';
import { AppList } from './AppList';
import { AppShow } from './AppShow';

export default {
    list: AppList,
    show: AppShow,
    edit: AppEdit,
    icon: AppIcon,
    recordRepresentation: record => record.name,
};
