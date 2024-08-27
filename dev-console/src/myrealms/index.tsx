import { RealmEdit } from './RealmEdit';
import { RealmIcon } from './RealmIcon';
import { RealmList, RealmSelectorList } from './RealmList';

export default {
    name: 'myrealms',
    list: RealmList,
    edit: RealmEdit,
    icon: RealmIcon,
    recordRepresentation: record => record.slug,
    selector: RealmSelectorList,
};
