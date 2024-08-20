import { useRecordContext } from 'react-admin';
import { Chip, Stack } from '@mui/material';
import { RoleIcon } from '../roles/RoleIcon';
import { GroupIcon } from '../group/GroupIcon';
import { AdminIcon, DeveloperIcon } from '../developers/DeveloperIcon';

export const TagsField = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <Stack direction={'row'} spacing={1}>
            {record.authorities &&
                record.authorities
                    .filter(a => a.role)
                    .map(a =>
                        a.role === 'ROLE_ADMIN' ? (
                            <Chip
                                label={a.role}
                                color="warning"
                                icon={<AdminIcon />}
                                key={'authority-' + a.role}
                            />
                        ) : a.role === 'ROLE_DEVELOPER' ? (
                            <Chip
                                label={a.role}
                                color="warning"
                                icon={<DeveloperIcon />}
                                key={'authority-' + a.role}
                            />
                        ) : (
                            <Chip
                                label={a.role}
                                color="warning"
                                key={'authority-' + a.role}
                            />
                        )
                    )}
            {record.groups?.map(g => (
                <Chip
                    label={g.group}
                    icon={<GroupIcon />}
                    color="secondary"
                    key={'group-' + g.group}
                />
            ))}
            {record.roles?.map(r => (
                <Chip
                    label={r.role}
                    icon={<RoleIcon />}
                    color="secondary"
                    key={'role-' + r.role}
                />
            ))}
        </Stack>
    );
};
