import * as React from 'react';
import {
    ShowBase,
    TextField,
    ReferenceField,
    ReferenceManyField,
    ReferenceArrayField,
    useRecordContext,
    useRedirect,
    LinearProgress,
    Labeled,
    DateField,
    FieldTitle,
    RichTextField,
} from 'react-admin';
import {
    Box,
    Dialog,
    DialogContent,
    Typography,
    Divider,
    Stack,
    Grid,
    Chip,
} from '@mui/material';
import { format } from 'date-fns';

export const AuditDetails = () => {
    const record = useRecordContext();
    if (!record || !record.data) {
        return <LinearProgress />;
    }
    return <UserAuthAuditDetails />;
};

const UserAuthAuditDetails = () => {
    const record: any = useRecordContext();
    if (!record || !record.data) {
        return <div></div>;
    }

    return (
        <Box sx={{ pb: 2 }}>
            <Stack direction="row" spacing={2}>
                <Labeled>
                    <DateField label="timestamp" source="timestamp" />
                </Labeled>

                <Labeled>
                    <TextField label="eventtype" source="type" />
                </Labeled>

                <Labeled>
                    <TextField label="principal" source="principal" />
                </Labeled>
            </Stack>

            {record.data && (
                <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
                    {record.data.provider && (
                        <Labeled>
                            <TextField
                                label="provider"
                                source="data.provider"
                            />
                        </Labeled>
                    )}
                    {record.data.authority && (
                        <Labeled>
                            <TextField
                                label="authority"
                                source="data.authority"
                            />
                        </Labeled>
                    )}
                    {/* {record.data.details.remoteAddress && (
                        <Labeled>
                            <TextField
                                label="remoteAddress"
                                source="data.details.remoteAddress"
                            />
                        </Labeled>
                    )} */}
                    {/* {record.data.details.protocol && (
                        <Labeled>
                            <TextField
                                label="protocol"
                                source="data.details.protocol"
                            />
                        </Labeled>
                    )}
                    {record.data.details.userAgent && (
                        <Labeled>
                            <TextField
                                label="userAgent"
                                source="data.details.userAgent"
                            />
                        </Labeled>
                    )} */}
                </Stack>
            )}
        </Box>
    );
};

const OAuth2TokenAuditDetails = () => {
    const record = useRecordContext();
    if (!record || !record.data) {
        return <div></div>;
    }

    return (
        <Box sx={{ pb: 2 }}>
            <Stack direction="row" spacing={2}>
                <Labeled>
                    <TextField label="client" source="data.authorizedParty" />
                </Labeled>

                <Labeled>
                    <TextField label="audience" source="data.audience" />
                </Labeled>
            </Stack>
            <Stack direction="row" spacing={2} sx={{ mt: 2 }}>
                <Labeled>
                    <DateField
                        label="issuedAt"
                        source="data.issuedAt"
                        showTime
                    />
                </Labeled>

                <Labeled>
                    <DateField
                        label="expiresAt"
                        source="data.expiration"
                        showTime
                    />
                </Labeled>
                <Stack component="span">
                    <Typography color="textSecondary">
                        <FieldTitle label="scope" />
                    </Typography>
                    <Grid container spacing={1}>
                        {record.data.scope.map(function (scope: any) {
                            return (
                                <Grid item key={record.id + '.scope.' + scope}>
                                    <Chip label={scope} />
                                </Grid>
                            );
                        })}
                    </Grid>
                </Stack>
            </Stack>
        </Box>
    );
};
