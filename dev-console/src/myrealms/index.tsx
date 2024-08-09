import { RealmEdit } from './RealmEdit';
import { RealmIcon } from './RealmIcon';
import { RealmList } from './RealmList';

export default {
    list: RealmList,
    edit: RealmEdit,
    icon: RealmIcon,
    recordRepresentation: record => record.slug,
};
