import {
    Box,
    Dialog,
    DialogContent,
    DialogTitle,
    Typography,
} from '@mui/material';
import {
    Button,
    Edit,
    RichTextField,
    ShowBase,
    SimpleShowLayout,
    TabbedShowLayout,
    TextField,
    TopToolbar,
    useRecordContext,
} from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import VisibilityIcon from '@mui/icons-material/Visibility';
import React from 'react';
import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/theme-github';

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
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="clientId" />
                    <TextField source="scopes" />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="OAuth2">
                    <RichTextField source="body" label={false} />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout>
        </>
    );
};

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
                <DialogTitle>Inpsect Json</DialogTitle>
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
        </TopToolbar>
    );
};
