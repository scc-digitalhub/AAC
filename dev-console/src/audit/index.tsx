import { AuditIcon } from './AuditIcon';
import { AuditList } from './AuditList';

export default {
    name: 'audit',
    list: AuditList,
    // icon: AuditIcon,
    recordRepresentation: record => record.id,
};
