import { AttributeSetIcon } from './AttributeSetIcon';
import { AttributeSetEdit } from './AttributeSetEdit';
import { AttributeSetList } from './AttributeSetList';

export default {
    name: 'attributeset',
    list: AttributeSetList,
    edit: AttributeSetEdit,
    icon: AttributeSetIcon,
    recordRepresentation: record => record.name,
};
