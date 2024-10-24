import { IdpEdit } from './IdpEdit';
import { IdpIcon } from './IdpIcon';
import { IdpList } from './IdpList';

export default {
    name: 'idps',
    list: IdpList,
    edit: IdpEdit,
    icon: IdpIcon,
    recordRepresentation: record => record.name,
};
