import {
    TextField,
    useRecordContext,
    LinearProgress,
    Labeled,
    DateField,
    FieldTitle,
} from 'react-admin';
import { Box, Typography, Stack, Grid, Chip } from '@mui/material';

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
                    <DateField
                        label="timestamp"
                        source="timestamp"
                        showTime
                        transform={(value: number) => new Date(value * 1000)}
                    />
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
                    {record.data.details &&
                        record.data.details.remoteAddress && (
                            <Labeled>
                                <TextField
                                    label="remoteAddress"
                                    source="data.details.remoteAddress"
                                />
                            </Labeled>
                        )}
                    {record.data.details && record.data.details.protocol && (
                        <Labeled>
                            <TextField
                                label="protocol"
                                source="data.details.protocol"
                            />
                        </Labeled>
                    )}
                    {record.data.details && record.data.details.userAgent && (
                        <Labeled>
                            <TextField
                                label="userAgent"
                                source="data.details.userAgent"
                            />
                        </Labeled>
                    )}
                </Stack>
            )}
        </Box>
    );
};