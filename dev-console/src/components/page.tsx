import { styled } from '@mui/material/styles';
import { Toolbar, Container, Paper, PaperProps } from '@mui/material';

export const Page = (props: PaperProps) => {
    const { children, className, ...rest } = props;

    return (
        <Container maxWidth={false} sx={{ pb: 2 }}>
            <StyledPaper className={className} {...rest}>
                {children}
            </StyledPaper>
        </Container>
    );
};

const StyledPaper = styled(Paper, {
    name: 'ContainerPaper',
})(({ theme }) => ({
    padding: theme.spacing(3),
    // flex: 1,
    // display: 'flex',
    // backgroundColor: theme.palette.grey[100],
}));

export default Page;
