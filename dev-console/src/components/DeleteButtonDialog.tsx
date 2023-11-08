import {
    Button,
    Confirm,
    DeleteWithConfirmButtonProps,
    RaRecord,
    SimpleForm,
    TextInput,
    required,
    useDeleteWithConfirmController,
    useRecordContext,
    useResourceContext,
} from 'react-admin';
import ActionDelete from '@mui/icons-material/Delete';
import { useState } from 'react';
/**
 * Delete with confirm button dialog
 *
 * @example
 * <DeleteButtonDialog
 *     confirmTitle="Item title"
 *     redirect="url to redirect after delete"
 * />
 */

const defaultIcon = <ActionDelete />;

export const DeleteButtonDialog = (
    props: CustomDeleteWithConfirmButtonProps
) => {
    const {
        className,
        confirmTitle = 'ra.message.delete_title',
        confirmContent = 'ra.message.delete_content',
        icon = defaultIcon,
        label = 'ra.action.delete',
        mutationMode = 'pessimistic',
        onClick,
        redirect = 'list',
        translateOptions = {},
        mutationOptions,
        color = 'error',
        source = 'id',
        meta,
        ...rest
    } = props;

    const resource = useResourceContext(props);
    const record = useRecordContext();

    const {
        open,
        isLoading,
        handleDialogOpen,
        handleDialogClose,
        handleDelete,
    } = useDeleteWithConfirmController({
        record,
        redirect,
        mutationMode,
        onClick,
        mutationOptions,
        resource,
    });

    const [value, setValue] = useState();

    const handleConfirm = (e: any) => {
        console.log(value);
        if (record.id === value) {
            console.log('MATCHED!!!!!');
            handleDelete(e);
        }
    };
    return (
        <>
            <span>
                <Button
                    label="Delete"
                    onClick={handleDialogOpen}
                    sx={{ color: 'red' }}
                >
                    {<ActionDelete />}
                </Button>
            </span>
            <Confirm
                isOpen={open}
                loading={isLoading}
                title={confirmTitle}
                content={
                    <DialogContent
                        setReturnValue={setValue}
                        id={record.id}
                        resourceName={resource}
                    />
                }
                onConfirm={handleConfirm}
                onClose={handleDialogClose}
            />
        </>
    );
};

export interface CustomDeleteWithConfirmButtonProps<
    RecordType extends RaRecord = any,
    MutationOptionsError = unknown
> extends DeleteWithConfirmButtonProps {
    source?: string;
    meta?: any;
}

const DialogContent = (params: any) => {
    const { setReturnValue, ...rest } = params;

    const idValidation = (value: any) => {
        // setReturnValue(value);
        if (!value) {
            return 'The id is required';
        }
        if (value !== params.id) {
            return 'Id does not match';
        }
        return undefined;
    };

    const validateClientId = [required(), setReturnValue, idValidation];
    let labelInput = params.resourceName + ' Id';

    return (
        <>
            <span>
                Are you sure you want to delete this {params.resourceName} ?
            </span>
            <br />
            <br />
            <span>
                You are deleting {params.resourceName}{' '}
                <span style={{ fontWeight: 500 }}>{params.id}</span>
            </span>
            <br />
            <span>To proceed enter the {params.resourceName} Id</span>
            <br />
            <br />
            <span style={{ color: 'red' }}>
                ATTENTION: This operation cannot be undone!
            </span>
            <SimpleForm toolbar={false} mode="onChange" reValidateMode="onBlur">
                <TextInput
                    label={labelInput}
                    source="confirmValue"
                    validate={validateClientId}
                    fullWidth
                />
            </SimpleForm>
        </>
    );
};
