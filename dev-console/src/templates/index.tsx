import { TemplateEdit } from './TemplateEdit';
import { TemplateIcon } from './TemplateIcon';
import { TemplateList } from './TemplateList';

export default {
    name: 'templatemodels',
    list: TemplateList,
    edit: TemplateEdit,
    icon: TemplateIcon,
    recordRepresentation: record => record.template + '_' + record.language,
};
