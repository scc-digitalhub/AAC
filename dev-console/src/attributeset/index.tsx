import { AttributeSetIcon } from './AttributeSetIcon';
import { AttributeSetEdit } from './AttributeSetEdit';
import { AttributeSetList } from './AttributeSetList';

export default {
    list: AttributeSetList,
    edit: AttributeSetEdit,
    icon: AttributeSetIcon,
    recordRepresentation: record => record.name,
};
