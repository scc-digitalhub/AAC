import {
    Box,
    Card,
    CardContent,
    Dialog,
    DialogContent,
    DialogTitle,
    Divider,
    Typography,
} from '@mui/material';
import {
    Button,
    Edit,
    EditBase,
    Form,
    RichTextField,
    SaveButton,
    ShowBase,
    SimpleShowLayout,
    TabbedShowLayout,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useEditContext,
    useNotify,
    useRecordContext,
    useRefresh,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import VisibilityIcon from '@mui/icons-material/Visibility';
import React from 'react';
import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-github';
import { CustomDeleteButtonDialog } from '../components/CustomDeleteButtonDialog';

export const AppEdit = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    return (
        <Edit
            actions={<EditToolBarActions />}
            mutationMode="pessimistic"
            queryOptions={options}
        >
            <AppTabComponent />
        </Edit>
    );
};

const AppTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;
    return (
        <>
            <br />
            <Typography variant="h5" sx={{ mr: 2, mt: 1 }}>
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" sx={{ mr: 2 }}>
                {record.id}
            </Typography>
            <br />
            <TabbedShowLayout sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Settings">
                    <TextField source="type" />
                    <TextField source="clientId" />
                    <TextField source="scopes" />
                    <EditSetting />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="OAuth2">
                    <RichTextField source="body" label={false} />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

const EditSetting = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const notify = useNotify();
    const refresh = useRefresh();
    const { isLoading, record } = useEditContext<any>();
    if (isLoading || !record) return null;
    const onSuccess = (data: any) => {
        notify(`App updated successfully`);
        refresh();
    };
    return (
        <EditBase
            mutationMode="pessimistic"
            mutationOptions={{ ...options, onSuccess }}
            queryOptions={options}
        >
            <Form>
                <Card>
                    <CardContent>
                        <Box>
                            <Box display="flex">
                                <Box flex="1" mt={-1}>
                                    <Box display="flex" width={430}>
                                        <TextInput source="name" fullWidth />
                                    </Box>
                                    <Box display="flex" width={430}>
                                        <TextInput
                                            source="description"
                                            fullWidth
                                        />
                                    </Box>
                                    <Divider />
                                </Box>
                            </Box>
                        </Box>
                    </CardContent>
                    <EditSettingToolbar />
                </Card>
            </Form>
        </EditBase>
    );
};

const EditSettingToolbar = (props: any) => (
    <Toolbar {...props}>
        <SaveButton />
    </Toolbar>
);

const EditToolBarActions = () => {
    const params = useParams();
    const options = { meta: { realmId: params.realmId } };
    const [open, setOpen] = React.useState(false);
    const record = useRecordContext();
    if (!record) return null;
    let body = JSON.stringify(record, null, '\t');
    const handleClick = () => {
        setOpen(true);
    };
    const handleClose = () => {
        setOpen(false);
    };
    return (
        <TopToolbar>
            <span>
                <Button
                    label="Inspect"
                    startIcon={<VisibilityIcon />}
                    onClick={handleClick}
                />
            </span>
            <Dialog
                open={open}
                onClose={handleClose}
                fullWidth
                maxWidth="md"
                sx={{
                    '.MuiDialog-paper': {
                        position: 'absolute',
                        top: 50,
                    },
                }}
            >
                <DialogTitle bgcolor={'#0066cc'} color={'white'}>
                    Inpsect Json
                </DialogTitle>
                <DialogContent>
                    <ShowBase queryOptions={options} id={params.id}>
                        {/* <RichTextField source={body} /> */}
                        <SimpleShowLayout>
                            <Typography>Raw JSON</Typography>
                            <Box>
                                <AceEditor
                                    setOptions={{
                                        useWorker: false,
                                    }}
                                    mode="json"
                                    value={body}
                                    width="100%"
                                    maxLines={20}
                                    wrapEnabled
                                    theme="github"
                                    showPrintMargin={false}
                                ></AceEditor>
                            </Box>
                        </SimpleShowLayout>
                    </ShowBase>
                </DialogContent>
            </Dialog>
            <CustomDeleteButtonDialog
                realmId={params.realmId}
                title="Client App Deletion"
                resourceName="Client Application"
            />
        </TopToolbar>
    );
};
