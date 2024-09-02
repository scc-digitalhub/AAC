import { ServiceEdit } from './ServiceEdit';
import { ServiceIcon } from './ServiceIcon';
import { ServiceList } from './ServiceList';

export default {
    name: 'services',
    list: ServiceList,
    edit: ServiceEdit,
    icon: ServiceIcon,
    recordRepresentation: record => record.name,
};
