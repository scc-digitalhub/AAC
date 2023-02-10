import {
    useTranslate,
    RaRecord,
    DateField,
    Labeled,
    TextField,
    DeleteWithConfirmButton,
    SimpleShowLayout,
    useRecordContext,
} from 'react-admin';
import { List } from 'react-admin';
import { Box, Typography, ListItem, Divider, Alert } from '@mui/material';
import AppShortcutIcon from '@mui/icons-material/AppShortcut';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';

import { List as MuiList } from '@mui/material';
import Accordion from '@mui/material/Accordion';
import AccordionSummary from '@mui/material/AccordionSummary';
import AccordionDetails from '@mui/material/AccordionDetails';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import GridList from '../components/gridList';
import { CardToolbar } from '../components/cardToolbar';

const getIcon = (record: any) => {
    if (record.expireDate && record.expireDate < Date.now()) {
        return (
            <WarningAmberIcon
                fontSize="large"
                sx={{ color: 'text.disabled' }}
            />
        );
    }

    return <AppShortcutIcon fontSize="large" />;
};

export const ConnectionsList = () => {
    return (
        <List
            component="div"
            resource="connections"
            pagination={false}
            actions={false}
        >
            <GridList
                cols={6}
                primaryText={record => {
                    return record.appName;
                }}
                icon={record => getIcon(record)}
                tertiaryText={record => record.clientId}
                secondaryText={<ConnectionsDetails />}
                actions={record => {
                    return (
                        <CardToolbar variant="dense" sx={{ width: 1 }}>
                            <DeleteWithConfirmButton
                                translateOptions={{ id: record.appName }}
                            />
                        </CardToolbar>
                    );
                }}
            />
        </List>
    );
};

export const ConnectionsDetails = () => {
    const translate = useTranslate();
    const record = useRecordContext();

    return (
        <SimpleShowLayout>
            {record.expireDate && record.expireDate < Date.now() && (
                <Alert severity="info" sx={{ mb: 1 }}>
                    {translate('alert.authorization_expired')}
                </Alert>
            )}
            {record.appDescription && (
                <Typography variant="subtitle1" sx={{ mb: 2 }}>
                    {record.appDescription}
                </Typography>
            )}

            <MuiList>
                <ListItem sx={{ pl: 0 }}>
                    <Labeled>
                        <DateField
                            label="created"
                            source="createDate"
                            showTime
                        />
                    </Labeled>
                </ListItem>
                <ListItem sx={{ pl: 0 }}>
                    <Labeled>
                        <DateField
                            label="expires"
                            source="expireDate"
                            showTime
                        />
                    </Labeled>
                </ListItem>
            </MuiList>

            {record.scopes && (
                <Box>
                    <Accordion elevation={0}>
                        <AccordionSummary
                            sx={{ pl: 0, m: 0 }}
                            expandIcon={<ExpandMoreIcon />}
                        >
                            <Typography variant="subtitle1">
                                {translate('page.connections.permissions_num', {
                                    num: record.scopes.length,
                                })}
                            </Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <MuiList>
                                {record.scopes.map((scope: RaRecord) => {
                                    scope.id = scope.scope;
                                    return (
                                        <ListItem
                                            sx={{ pl: 0 }}
                                            key={
                                                record.id + '.scope.' + scope.id
                                            }
                                        >
                                            <Box>
                                                <Typography color="textSecondary">
                                                    <TextField
                                                        source="scope"
                                                        resource="scope"
                                                        record={scope}
                                                    />
                                                </Typography>
                                                <Typography
                                                    variant="subtitle1"
                                                    sx={{
                                                        fontWeight: 600,
                                                    }}
                                                >
                                                    {scope.name}
                                                </Typography>
                                                <TextField
                                                    label="description"
                                                    source="description"
                                                    resource="scope"
                                                    record={scope}
                                                />
                                                <Divider variant="fullWidth" />
                                            </Box>
                                        </ListItem>
                                    );
                                })}
                            </MuiList>
                        </AccordionDetails>
                    </Accordion>
                </Box>
            )}
        </SimpleShowLayout>
    );
};
