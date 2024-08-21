import { AuditIcon } from './AuditIcon';
import { AuditList } from './AuditList';

export default {
    list: AuditList,
    // icon: AuditIcon,
    recordRepresentation: record => record.id,
};
