import { useRecordContext } from 'react-admin';
import { Chip, Stack } from '@mui/material';
import { RoleIcon } from '../roles/RoleIcon';
import { GroupIcon } from '../group/GroupIcon';
import DeveloperIcon from '@mui/icons-material/DeveloperMode';
import AdminIcon from '@mui/icons-material/AdminPanelSettings';

export const TagsField = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <Stack direction={'row'} spacing={1}>
            {record.authorities
                .filter(a => a.role)
                .map(a =>
                    a.role === 'ROLE_ADMIN' ? (
                        <Chip
                            label={a.role}
                            color="warning"
                            icon={<AdminIcon />}
                        />
                    ) : a.role === 'ROLE_DEVELOPER' ? (
                        <Chip
                            label={a.role}
                            color="warning"
                            icon={<DeveloperIcon />}
                        />
                    ) : (
                        <Chip label={a.role} color="warning" />
                    )
                )}
            {record.groups.map(g => (
                <Chip label={g.group} icon={<GroupIcon />} color="secondary" />
            ))}
            {record.roles.map(r => (
                <Chip label={r.role} icon={<RoleIcon />} color="secondary" />
            ))}
        </Stack>
    );
};
