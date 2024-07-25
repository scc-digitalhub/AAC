import { Typography } from '@mui/material';
import { Edit, ShowButton, TopToolbar, useRecordContext } from 'react-admin';
import { useParams } from 'react-router-dom';
import StarBorderIcon from '@mui/icons-material/StarBorder';
import React from 'react';
import { DeleteButtonDialog } from '../components/DeleteButtonDialog';
import { RJSFSchema, UiSchema } from '@rjsf/utils';
import { ExportRecordButton } from '@dslab/ra-export-record-button';
import { InspectButton } from '@dslab/ra-inspect-button';
import { EnableIdpButton } from './IdpList';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/page';

export const IdpEdit = () => {
    return (
        <Page>
            <Edit actions={<EditToolBarActions />} mutationMode="pessimistic">
                <IdpTabComponent />
            </Edit>
        </Page>
    );
};

const IdpTabComponent = () => {
    const record = useRecordContext();
    if (!record) return null;

    // return <IdpTabs id={record.provider}></IdpTabs>;

    return (
        <>
            <br />
            <Typography variant="h5" sx={{ ml: 2, mt: 1 }}>
                <StarBorderIcon color="primary" /> {record.name}
            </Typography>
            <Typography variant="h6" sx={{ ml: 2 }}>
                {record.provider}
            </Typography>
            <br />
            {/* <TabbedShowLayout sx={{ mr: 1 }} syncWithLocation={false}>
                <TabbedShowLayout.Tab label="Internal Configuration">
                    <TextField source="name" />
                    <TextField source="type" />
                    <TextField source="authority" />
                    <TextField source="provider" />
                    <TextField source="enabled" />
                    <TextField source="registered" />
                    <InternalConfiguration />
                </TabbedShowLayout.Tab>
                <TabbedShowLayout.Tab label="Settings">
                    <Typography variant="h5" sx={{ mr: 2 }}>
                        OAuth2.0 Configuration
                    </Typography>
                    <Typography variant="h6" sx={{ mr: 2 }}>
                        Basic client configuration for OAuth2/OpenId Connect
                    </Typography>
                    <EditOAuthJsonSchemaForm />
                </TabbedShowLayout.Tab>
            </TabbedShowLayout> */}
        </>
    );
};

const EditToolBarActions = () => {
    const params = useParams();
    const [open, setOpen] = React.useState(false);
    const record = useRecordContext();
    if (!record) return null;
    return (
        <TopToolbar>
            <EnableIdpButton />
            <InspectButton />

            {/* <Button label="Inspect" onClick={handleClick}>
                {<VisibilityIcon />}
            </Button>
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
            </Dialog> */}
            <DeleteWithDialogButton />
            <ExportRecordButton />
        </TopToolbar>
    );
};
