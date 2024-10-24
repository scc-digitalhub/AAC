import { DeveloperIcon } from './DeveloperIcon';
import { DeveloperList } from './DeveloperList';

export default {
    icon: DeveloperIcon,
    list: DeveloperList,
    recordRepresentation: record => record.subjectId,
};
